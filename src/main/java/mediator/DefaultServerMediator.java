package mediator;

import factory.ErrorResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

public class DefaultServerMediator implements ServerMediator {
    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        ErrorResponseCreator creator = new ErrorResponseCreator();
        return creator.createResponse(503, "<h1>Service Unavailable</h1><p>Server mediator not configured.</p>");
    }

    @Override
    public void logRequest(HttpRequest request) {}
}
