package server;

import model.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class Statistics {
    private List<HttpRequest> requests = new ArrayList<>();

    public void LogRequest(HttpRequest req) {
        requests.add(req);
        System.out.println("Logged request: " + req.getMethod() + " " + req.getUrl());
    }

    public int getTotalRequests() {
        return requests.size();
    }
}
