package server;

import builder.HttpResponseBuilder;
import builder.HttpResponseDirector;
import model.HttpRequest;
import model.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RequestHandler {
    private HttpServer server;

    public RequestHandler(HttpServer server) {
        this.server = server;
    }

    public HttpResponse Handle(HttpRequest req) {
        String url = req.getUrl();
        String body;

        if (url.equals("/home")) body = "<h1>Welcome to Home!</h1>";
        else if (url.equals("/about")) body = "<h1>About us page.</h1>";
        else if (url.equals("/contact")) body = "<h1>Contact information here.</h1>";
        else body = "<h1>404 Page Not Found</h1>";

        int statusCode = url.equals("/home") || url.equals("/about") || url.equals("/contact") ? 200 : 404;

        HttpResponseDirector director = new HttpResponseDirector(new HttpResponseBuilder());

        HttpResponse response = (statusCode == 200)
                ? director.createSuccessResponse(body)
                : director.createErrorResponse(statusCode, body);

        response.getHeaders().put("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));

        return response;
    }
}