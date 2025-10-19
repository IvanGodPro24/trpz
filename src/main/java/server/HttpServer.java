package server;

import mediator.ServerMediator;
import model.HttpRequest;
import model.HttpResponse;
import state.IServerState;
import state.InitializingState;

public class HttpServer {
    private IServerState state;
    private final int port;
    private ServerMediator mediator;

    public HttpServer(int port, ServerMediator mediator) {
        this.port = port;
        this.state = new InitializingState();
        this.mediator = mediator;
    }

    public void SetState(IServerState newState) {
        this.state = newState;
    }

    public void Start() {
        state.Start(this);
    }

    public void Stop() {
        state.Stop(this);
    }

    public HttpResponse HandleRequest(HttpRequest req) {
        return state.HandleRequest(this, req);
    }

    public int getPort() {
        return port;
    }

    public void setMediator(ServerMediator mediator) {
        this.mediator = mediator;
    }

    public ServerMediator getMediator() {
        return mediator;
    }
}

