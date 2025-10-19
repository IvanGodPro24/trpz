package state;

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
        return mediator.handleRequest(request);
    }
}