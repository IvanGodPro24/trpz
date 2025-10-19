package state;

import factory.ErrorResponseCreator;
import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;

public class InitializingState implements IServerState {
    @Override
    public void Start(HttpServer server) {
        System.out.println("Starting server on port " + server.getPort() + "...");
        server.SetState(new OpenState());
        System.out.println("Server is now running.");
    }

    @Override
    public void Stop(HttpServer server) {
        System.out.println("Cannot stop while initializing.");
    }

    @Override
    public HttpResponse HandleRequest(HttpServer server, HttpRequest request) {
        System.out.println("Request ignored: server is initializing.");

        ErrorResponseCreator factory = new ErrorResponseCreator();
        return factory.createResponse(
                503,
                "<h1>Server is initializing, please wait.</h1>"
        );
    }
}