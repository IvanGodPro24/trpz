package mediator;

import factory.ErrorResponseCreator;
import model.HttpRequest;
import model.HttpResponse;
import server.RequestHandler;
import server.Statistics;

public class DefaultServerMediator implements ServerMediator {
    private final RequestHandler handlerFallback = null;
    private final Statistics statsFallback = null;

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        ErrorResponseCreator creator = new ErrorResponseCreator();
        return creator.createResponse(503, "<h1>Service Unavailable</h1><p>Server mediator not configured.</p>");
    }

    @Override
    public void logRequest(HttpRequest request) {}
}
