package httpserver;

import model.HttpRequest;
import model.HttpResponse;
import server.HttpServer;

public class Main {
    static void main() {
        HttpServer server = new HttpServer(8080);

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

