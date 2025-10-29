package p2p;

import db.MongoDBConnection;
import db.StatisticsRepository;
import mediator.ConcreteServerMediator;
import mediator.ServerMediator;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;
import server.Statistics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class PeerNode {
    private final String name;
    private final HttpServer server;
    private final PeerClient client;
    private final Statistics stats;
    private final List<String> peers;

    public PeerNode(String name, int port, List<String> peers) {
        this.name = name;

        MongoDBConnection.initialize();

        StatisticsRepository repository = new StatisticsRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        this.stats = new Statistics(repository);
        RequestHandler handler = new RequestHandler(this.stats);
        this.server = new HttpServer(port, null);
        ServerMediator mediator = new ConcreteServerMediator(server, handler, stats);
        this.client = new PeerClient();
        this.peers = peers;
    }

    public void start() {
        System.out.println(name + " starting on port " + server.getPort());
        server.Start();
    }

    public void stop() {
        server.Stop();
    }

    public void broadcastRequest(String path) {
        for (String peerUrl : peers) {
            try {
                System.out.println(name + " sending request to " + peerUrl + path);
                HttpResponse response = client.sendGet(peerUrl, path);
                System.out.println("Response from " + peerUrl + ":\n" + response);
            } catch (IOException | URISyntaxException e) {
                System.err.println("Peer " + peerUrl + " unreachable: " + e.getMessage());
            }
        }
    }

    public void syncWithPeers() {
        for (String peerUrl : peers) {
            try {
                HttpResponse resp = client.sendGet(peerUrl, "/sync/stats");
                String body = resp.body() == null ? "" : resp.body().trim();
                if (!body.isEmpty()) {
                    int added = stats.mergeFromTimestampsJson(body);
                    System.out.println(name + " merged " + added + " events from " + peerUrl);
                } else {
                    System.out.println(name + " got empty body from " + peerUrl);
                }
            } catch (IOException | URISyntaxException e) {
                System.err.println("Failed sync with " + peerUrl + ": " + e.getMessage());
            }
        }
    }

    public void syncStatsToPeersPost() {
        String json = stats.toJsonTimestamps();
        for (String peerUrl : peers) {
            try {
                System.out.println(name + " syncing stats to " + peerUrl + " -> " + json);
                HttpResponse resp = client.sendPost(peerUrl, "/sync/stats", json, "application/json; charset=utf-8");
                System.out.println("Sync response from " + peerUrl + ": status=" + resp.statusCode());
            } catch (IOException | URISyntaxException e) {
                System.err.println("Failed to sync to " + peerUrl + ": " + e.getMessage());
            }
        }
    }
}