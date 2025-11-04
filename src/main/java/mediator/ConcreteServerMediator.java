package mediator;

import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;

public class ConcreteServerMediator implements ServerMediator {
    private final RequestHandler handler;

    public ConcreteServerMediator(HttpServer server, RequestHandler handler) {
        this.handler = handler;

        server.setMediator(this);
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        return handler.Handle(request);
    }
}