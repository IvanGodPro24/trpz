package state;

import factory.ErrorResponseCreator;
import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;

public class ClosingState implements IServerState {
    @Override
    public void Start(HttpServer server) {
        System.out.println("Cannot start: server is shutting down.");
    }

    @Override
    public void Stop(HttpServer server) {
        System.out.println("Server is already shutting down.");
    }

    @Override
    public HttpResponse HandleRequest(HttpServer server, HttpRequest request) {
        System.out.println("Request rejected: server is closing.");

        ErrorResponseCreator factory = new ErrorResponseCreator();
        return factory.createResponse(
                503,
                "<h1>Server is currently closing, please try again later.</h1>"
        );
    }
}