package server;

import mediator.ServerMediator;
import model.HttpRequest;
import model.HttpResponse;
import state.IServerState;
import state.InitializingState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private volatile IServerState state;
    private final int port;
    private ServerMediator mediator;
    private volatile boolean running = false;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public HttpServer(int port, ServerMediator mediator) {
        this.port = port;
        this.state = new InitializingState();
        this.mediator = mediator;
    }

    public synchronized void SetState(IServerState newState) {
        this.state = newState;
    }

    public synchronized void Start() {
        state.Start(this);
        running = true;

        executorService = Executors.newFixedThreadPool(10);

        serverThread = new Thread(this::listen, "ServerThread-" + port);
        serverThread.start();
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            System.out.println("Listening for connections on port " + port + "...");
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(new HttpServerWorker(clientSocket, this));
                } catch (IOException e) {
                    if (running)
                        System.err.println("Accept error on port " + port + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
        } finally {
            System.out.println("Server listener on port " + port + " exited.");
        }
    }

    public synchronized void Stop() {
        running = false;
        state.Stop(this);
        System.out.println("Stopping server on port " + port + "...");

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        try {
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.join(1000);
            }
        } catch (InterruptedException ignored) {}

        System.out.println("Server on port " + port + " stopped.");
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
