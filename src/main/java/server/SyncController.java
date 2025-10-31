package server;

import factory.HttpResponseCreator;
import model.HttpRequest;
import model.HttpResponse;
import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import factory.ErrorResponseCreator;
import factory.SuccessResponseCreator;

public class SyncController implements IController {
    private final Statistics stats;

    public SyncController(Statistics stats) {
        this.stats = stats;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        String method = req.method().toUpperCase();
        if ("GET".equals(method)) {
            String body = stats.toJsonTimestamps();
            IHttpResponseBuilder builder = new HttpResponseBuilder();
            return builder
                    .setStatusCode(200)
                    .setHeader("Server", "JavaHTTP/1.0")
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody(body)
                    .build();
        } else if ("POST".equals(method)) {
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
        } else {
            return new ErrorResponseCreator().createResponse(405, "<h1>Method Not Allowed</h1>");
        }
    }
}