package server;

import builder.HttpResponseBuilder;
import builder.HttpResponseDirector;
import model.HttpRequest;
import model.HttpResponse;

public class RequestHandler {
    private final HttpServer server;

    public RequestHandler(HttpServer server) {
        this.server = server;
    }

    public HttpResponse Handle(HttpRequest req) {
        String url = req.getUrl();
        String body = switch (url) {
            case "/home" -> "<h1>Welcome to Home!</h1>";
            case "/about" -> "<h1>About us page.</h1>";
            case "/contact" -> "<h1>Contact information here.</h1>";
            default -> "<h1>404 Page Not Found</h1>";
        };

        int statusCode = url.equals("/home") || url.equals("/about") || url.equals("/contact") ? 200 : 404;

        HttpResponseDirector director = new HttpResponseDirector(new HttpResponseBuilder());

        return (statusCode == 200)
                ? director.createSuccessResponse(body)
                : director.createErrorResponse(statusCode, body);
        }
}