package server;

import chtml.ChtmlEngine;
import db.RequestsRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import composite.HtmlComposite;
import composite.HtmlElement;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.SuccessResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

import static http.HttpUtils.*;

public class RequestHandler {
    private final Statistics stats;
    private final RequestsRepository requestsRepo;

    public RequestHandler(Statistics stats, RequestsRepository requestsRepo) {
        this.stats = stats;
        this.requestsRepo = requestsRepo;
    }

    public HttpResponse Handle(HttpRequest req) {
        String url = req.url();
        String method = req.method();

        if ("POST".equalsIgnoreCase(method) && "/sync/stats".equalsIgnoreCase(url)) {
            String body = req.body();
            int added = 0;
            if (body != null && !body.isBlank()) {
                try {
                    added = stats.mergeFromTimestampsJson(body);
                } catch (Exception e) {
                    System.err.println("Failed to merge timestamps from peer: " + e.getMessage());
                }
            }
            String respBody = "<h1>OK</h1><p>merged=" + added + "</p>";
            HttpResponseCreator creator = new SuccessResponseCreator();
            return creator.createResponse(200, respBody);
        }

        if ("GET".equalsIgnoreCase(method) && "/sync/stats".equalsIgnoreCase(url)) {
            String body = stats.toJsonTimestamps();

            IHttpResponseBuilder builder = new HttpResponseBuilder();
            return builder
                    .setStatusCode(200)
                    .setHeader("Server", "JavaHTTP/1.0")
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody(body)
                    .build();
        }

        if ("GET".equalsIgnoreCase(method) && url.startsWith("/templates/")) {
            String tplNameRaw = url.substring("/templates/".length());
            int qidx = tplNameRaw.indexOf('?');
            String tplPart = qidx >= 0 ? tplNameRaw.substring(0, qidx) : tplNameRaw;
            String tplName = tplPart.replaceAll("\\.\\./", "").replaceAll("^/+", "");
            try {
                tplName = java.net.URLDecoder.decode(tplName, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {}

            try {
                Map<String, Object> ctx = parseQueryToContext(req);
                ctx.putIfAbsent("name", "Guest");
                ctx.putIfAbsent("count", 0);
                String out = ChtmlEngine.render(tplName, ctx);
                HttpResponseCreator creator = new SuccessResponseCreator();
                return creator.createResponse(200, out);
            } catch (Exception e) {
                ErrorResponseCreator err = new ErrorResponseCreator();
                String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
                return err.createResponse(500, msg);
            }
        }


        if ("GET".equalsIgnoreCase(method) && "/contact".equalsIgnoreCase(url)) {
            try {
                Map<String, Object> ctx = new HashMap<>();
                String out = ChtmlEngine.render("contact.chtml", ctx);
                HttpResponseCreator creator = new SuccessResponseCreator();
                return creator.createResponse(200, out);
            } catch (Exception e) {
                ErrorResponseCreator err = new ErrorResponseCreator();
                String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
                return err.createResponse(500, msg);
            }
        }

        if ("POST".equalsIgnoreCase(method) && "/contact".equalsIgnoreCase(url)) {
            String rawBody = req.body() == null ? "" : req.body();
            Map<String, Object> ctx = parseFormUrlEncoded(rawBody);

            String name = safeToString(ctx.get("name")).trim();
            String email = safeToString(ctx.get("email")).trim();
            String message = safeToString(ctx.get("message")).trim();

            String info = null;
            if (name.isEmpty()) {
                info = "Name is required.";
            } else if (email.isEmpty()) {
                info = "Email is required.";
            } else if (!email.contains("@")) {
                info = "Email looks invalid.";
            }

            if (info != null) {
                ctx.put("info", info);
                ctx.put("name", name);
                ctx.put("email", email);
                ctx.put("message", message);
                try {
                    String out = ChtmlEngine.render("contact.chtml", ctx);
                    HttpResponseCreator creator = new SuccessResponseCreator();
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
                    requestsRepo.saveContact(name, email, message, receivedAt, clientIp);
                }
            } catch (Exception e) {
                System.err.println("Error saving contact to DB: " + e.getMessage());
            }

            try {
                String out = ChtmlEngine.render("contact_response.chtml", ctx);
                HttpResponseCreator creator = new SuccessResponseCreator();
                return creator.createResponse(200, out);
            } catch (Exception e) {
                ErrorResponseCreator err = new ErrorResponseCreator();
                String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
                return err.createResponse(500, msg);
            }
        }

        String body = switch (url) {
            case "/home" -> buildHomePage();
            case "/about" -> buildAboutPage();
            case "/stats" -> buildStatsPage();
            default -> "<h1>404 Page Not Found</h1>";
        };

        int statusCode = (url.equals("/home") || url.equals("/about") || url.equals("/stats")) ? 200 : 404;

        HttpResponseCreator creator = (statusCode == 200)
                ? new SuccessResponseCreator()
                : new ErrorResponseCreator();

        return creator.createResponse(statusCode, body);
    }

    private String buildHomePage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Welcome to Home!"));
        body.add(new HtmlElement("p", "This page is generated using the Composite pattern."));
        html.add(body);
        return html.render();
    }

    private String buildAboutPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "About Us"));
        body.add(new HtmlElement("p", "This page demonstrates Composite usage for dynamic HTML."));
        html.add(body);
        return html.render();
    }

    private String buildStatsPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Server statistics"));
        body.add(new HtmlElement("p", "Total requests: " + stats.getTotalRequests()));
        html.add(body);
        return html.render();
    }
}
