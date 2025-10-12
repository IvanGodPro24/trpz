package server;

import model.HttpRequest;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class Statistics {
    private final List<HttpRequest> requests = new CopyOnWriteArrayList<>();

    public void LogRequest(HttpRequest req) {
        requests.add(req);
        System.out.println("Logged request: " + req.getMethod() + " " + req.getUrl());
    }

    public int getTotalRequests() {
        return requests.size();
    }
}
