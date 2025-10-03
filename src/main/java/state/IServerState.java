package state;

import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;

public interface IServerState {
    void Start(HttpServer server);
    void Stop(HttpServer server);
    HttpResponse HandleRequest(HttpServer server, HttpRequest request);
}
