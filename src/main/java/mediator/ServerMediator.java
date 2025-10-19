package mediator;

import model.HttpRequest;
import model.HttpResponse;

public interface ServerMediator {
    HttpResponse handleRequest(HttpRequest request);
    void logRequest(HttpRequest request);
}
