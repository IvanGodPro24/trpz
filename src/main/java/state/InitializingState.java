package state;

import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;
import java.util.HashMap;

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

        return new HttpResponse(
                503,
                "Service Unavailable",
                new HashMap<>(),
                "Server is initializing, please wait."
        );
    }
}