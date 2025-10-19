package httpserver;

import mediator.ConcreteServerMediator;
import mediator.ServerMediator;
import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;
import server.Statistics;

public class Main {
    static void main() {
        Statistics statistics = new Statistics();
        RequestHandler handler = new RequestHandler(null);
        HttpServer server = new HttpServer(8080, null);
        ServerMediator mediator = new ConcreteServerMediator(server, handler, statistics);

        server.setMediator(mediator);

        // Initializing
        HttpResponse r1 = server.HandleRequest(new HttpRequest("GET", "/home"));
        System.out.println("Response1: " + r1);

        server.Start();

        // Open
        HttpResponse r2 = server.HandleRequest(new HttpRequest("GET", "/about"));
        System.out.println("Response2:\n" + r2);

        server.Stop();

        // Closing
        HttpResponse r3 = server.HandleRequest(new HttpRequest("GET", "/contact"));
        System.out.println("Response3: " + r3);
    }
}

