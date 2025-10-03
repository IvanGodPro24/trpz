package server;

import model.HttpRequest;
import model.HttpResponse;

public class RequestHandler {
    private HttpServer server;

    public RequestHandler(HttpServer server) {
        this.server = server;
    }

    public HttpResponse Handle(HttpRequest req) {
        String url = req.getUrl();
        String body = "Default response";

        if (url.equals("/home")) body = "Welcome to Home!";
        else if (url.equals("/about")) body = "About us page.";
        else if (url.equals("/contact")) body = "Contact information here.";

        return new HttpResponse(200, body);
    }
}
