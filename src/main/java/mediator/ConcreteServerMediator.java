package mediator;

import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;
import server.Statistics;

public class ConcreteServerMediator implements ServerMediator {
    private final RequestHandler handler;
    private final Statistics statistics;

    public ConcreteServerMediator(HttpServer server, RequestHandler handler, Statistics statistics) {
        this.handler = handler;
        this.statistics = statistics;

        server.setMediator(this);
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        logRequest(request);
        return handler.Handle(request);
    }

    @Override
    public void logRequest(HttpRequest request) {
        statistics.logRequest(request);
    }
}