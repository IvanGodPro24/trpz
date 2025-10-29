package state;

import factory.ErrorResponseCreator;
import mediator.ServerMediator;
import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;

public class OpenState implements IServerState {
    @Override
    public void Start(HttpServer server) {
        System.out.println("Server already running.");
    }

    @Override
    public void Stop(HttpServer server) {
        System.out.println("Shutting down server...");
        server.SetState(new ClosingState());
    }

    @Override
    public HttpResponse HandleRequest(HttpServer server, HttpRequest request) {
        ServerMediator mediator = server.getMediator();

        if (mediator == null) {
            ErrorResponseCreator factory = new ErrorResponseCreator();
            return factory.createResponse(503, "<h1>Server not ready</h1>");
        }
        return mediator.handleRequest(request);
    }
}