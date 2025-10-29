package server;

import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import composite.HtmlComposite;
import composite.HtmlElement;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.SuccessResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

public class RequestHandler {
    private final Statistics stats;

    public RequestHandler(Statistics stats) {
        this.stats = stats;
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

    String body = switch (url) {
            case "/home" -> buildHomePage();
            case "/about" -> buildAboutPage();
            case "/contact" -> buildContactPage();
            case "/stats" -> buildStatsPage();
            default -> "<h1>404 Page Not Found</h1>";
        };

        int statusCode = (url.equals("/home") || url.equals("/about") || url.equals("/contact") || url.equals("/stats")) ? 200 : 404;

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

    private String buildContactPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Contact information"));
        body.add(new HtmlElement("p", "Email: contact@server.com"));
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
