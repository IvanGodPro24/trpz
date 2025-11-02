package server;

import chtml.ChtmlEngine;
import db.RequestsRepository;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.TemplateResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

import static http.HttpUtils.*;

public class ContactController implements IController {
    private final RequestsRepository requestsRepo;

    public ContactController(RequestsRepository requestsRepo) {
        this.requestsRepo = requestsRepo;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        String method = req.method().toUpperCase();
        return switch (method) {
            case "GET" -> handleGet();
            case "POST" -> handlePost(req);
            case "PUT" -> handlePut(req);
            case "DELETE" -> handleDelete(req);
            default -> new ErrorResponseCreator().createResponse(405, "<h1>Method Not Allowed</h1>");
        };
    }

    private HttpResponse handleGet() {
        try {
            Map<String, Object> ctx = new HashMap<>();
            String out = ChtmlEngine.render("contact.chtml", ctx);

            HttpResponseCreator creator = new TemplateResponseCreator();
            return creator.createResponse(200, out);
        } catch (Exception e) {
            ErrorResponseCreator err = new ErrorResponseCreator();
            String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
            return err.createResponse(500, msg);
        }
    }

    private HttpResponse handlePost(HttpRequest req) {
        return processCreateOrUpdate(req, false);
    }

    private HttpResponse handlePut(HttpRequest req) {
        return processCreateOrUpdate(req, true);
    }


    private HttpResponse handleDelete(HttpRequest req) {
        String id = null;

        Map<String, Object> qctx = parseQueryToContext(req);
        if (qctx.containsKey("id")) id = safeToString(qctx.get("id"));

        if ((id == null || id.isBlank())) {
            req.headers();
            id = req.headers().getOrDefault("X-Id", req.headers().get("x-id"));
        }

        String rawBody = req.body() == null ? "" : req.body().trim();
        if ((id == null || id.isBlank()) && !rawBody.isEmpty()) {
            Map<String, Object> j = tryParseJsonFlat(rawBody);
            if (j.containsKey("id")) id = safeToString(j.get("id"));
        }

        if (id != null && !id.isBlank()) {
            try {
                boolean ok = requestsRepo != null && requestsRepo.deleteById(id);
                if (ok) return new TemplateResponseCreator().createResponse(200, "<h1>Deleted</h1><p>id=" + escapeHtml(id) + "</p>");
                else return new ErrorResponseCreator().createResponse(404, "<h1>Not Found</h1><p>id=" + escapeHtml(id) + "</p>");
            } catch (Exception e) {
                System.err.println("Error deleting id=" + id + " : " + e.getMessage());
                return new ErrorResponseCreator().createResponse(500, "<h1>Internal Server Error</h1>");
            }
        }

        String adminHeader = getHeaderIgnoreCase(req.headers(), "X-Admin");
        boolean admin = "true".equalsIgnoreCase(adminHeader);

        String rawUrl = req.url();
        if (!admin && rawUrl != null) {
            int q = rawUrl.indexOf('?');
            if (q >= 0) {
                String qs = rawUrl.substring(q + 1);
                Map<String, Object> ctx = parseQueryToContext("?" + qs);
                Object a = ctx.get("admin");
                if (a != null && (a.toString().equals("1") || a.toString().equalsIgnoreCase("true"))) admin = true;
            }
        }

        if (!admin) {
            return new ErrorResponseCreator().createResponse(403, "<h1>Forbidden</h1><p>Admin header required to delete.</p>");
        }

        try {
            if (requestsRepo != null) requestsRepo.clearAll();
            return new TemplateResponseCreator().createResponse(200, "<h1>All contact requests cleared</h1>");
        } catch (Exception e) {
            System.err.println("Error clearing requests: " + e.getMessage());
            return new ErrorResponseCreator().createResponse(500, "<h1>Internal Server Error</h1>");
        }
    }

    private HttpResponse processCreateOrUpdate(HttpRequest req, boolean isPut) {
        String rawBody = req.body() == null ? "" : req.body();
        Map<String, Object> ctx;

        String contentType = getHeaderIgnoreCase(req.headers(), "Content-Type");
        boolean isJson = contentType != null && contentType.toLowerCase().contains("application/json");

        if (isJson) {
            ctx = tryParseJsonFlat(rawBody);
        } else {
            ctx = parseFormUrlEncoded(rawBody);
        }

        String id = safeToString(ctx.get("id")).trim();
        String name = safeToString(ctx.get("name")).trim();
        String email = safeToString(ctx.get("email")).trim();
        String message = safeToString(ctx.get("message")).trim();

        String info = null;
        if (name.isEmpty()) info = "Name is required.";
        else if (email.isEmpty()) info = "Email is required.";
        else if (!email.contains("@")) info = "Email looks invalid.";

        if (info != null) {
            ctx.put("info", info);
            ctx.put("name", name);
            ctx.put("email", email);
            ctx.put("message", message);
            try {
                String out = ChtmlEngine.render("contact.chtml", ctx);
                HttpResponseCreator creator = new TemplateResponseCreator();
                return creator.createResponse(400, out);
            } catch (Exception e) {
                ErrorResponseCreator err = new ErrorResponseCreator();
                String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
                return err.createResponse(500, msg);
            }
        }

        String receivedAt = Instant.now().toString();
        ctx.put("receivedAt", receivedAt);
        ctx.put("name", name);
        ctx.put("email", email);
        ctx.put("message", message);

        try {
            if (requestsRepo != null) {
                String clientIp = req.headers().getOrDefault("X-Forwarded-For", req.headers().get("X-Real-IP"));

                if (!id.isBlank()) {
                    boolean updated = requestsRepo.updateContact(id, name, email, message, receivedAt);
                    if (updated) {
                        String out = ChtmlEngine.render("contact_response.chtml", ctx);
                        return new TemplateResponseCreator().createResponse(200, out);
                    } else {
                        if (isPut) {
                            // PUT
                            return new ErrorResponseCreator().createResponse(404, "<h1>Not Found</h1><p>id=" + escapeHtml(id) + "</p>");
                        } else {
                            // POST
                            requestsRepo.saveContact(name, email, message, receivedAt, clientIp);
                        }
                    }
                } else {
                    // create new
                    requestsRepo.saveContact(name, email, message, receivedAt, clientIp);
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving contact to DB: " + e.getMessage());
        }

        try {
            String out = ChtmlEngine.render("contact_response.chtml", ctx);

            HttpResponseCreator creator = new TemplateResponseCreator();

            return creator.createResponse(200, out);
        } catch (Exception e) {
            ErrorResponseCreator err = new ErrorResponseCreator();
            String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
            return err.createResponse(500, msg);
        }
    }

    private static Map<String, Object> tryParseJsonFlat(String raw) {
        Map<String, Object> m = new HashMap<>();
        if (raw == null) return m;
        String s = raw.trim();
        if (s.isEmpty()) return m;
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        String[] pairs = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String p : pairs) {
            String[] kv = p.split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length() - 1);
            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
            v = v.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
            m.put(k, v);
        }
        return m;
    }
}
