package server;

import chtml.ChtmlEngine;
import db.RequestsRepository;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.SuccessResponseCreator;
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
        if ("GET".equals(method)) {
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
        } else if ("POST".equals(method)) {
            String rawBody = req.body() == null ? "" : req.body();
            Map<String, Object> ctx = parseFormUrlEncoded(rawBody);

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
        } else {
            return new ErrorResponseCreator().createResponse(405, "<h1>Method Not Allowed</h1>");
        }
    }
}
