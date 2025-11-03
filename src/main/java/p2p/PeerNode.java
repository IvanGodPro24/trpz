package p2p;

import db.MongoDBConnection;
import db.StatisticsRepository;
import db.RequestsRepository;
import db.PeersRepository;
import mediator.ConcreteServerMediator;
import mediator.ServerMediator;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;
import server.Statistics;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.List;

public class PeerNode {
    private final String name;
    private final HttpServer server;
    private final PeerClient client;
    private final Statistics stats;
    private final PeerNetwork peerNetwork;
    private final List<String> peers;

    public PeerNode(String name, int port, List<String> seedPeers) {
        this.name = name;
        this.peers = seedPeers;

        MongoDBConnection.initialize();

        StatisticsRepository statisticsRepo = new StatisticsRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        RequestsRepository requestsRepo = new RequestsRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        PeersRepository peersRepo = new PeersRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        this.stats = new Statistics(statisticsRepo);
        this.client = new PeerClient();
        this.server = new HttpServer(port, null);

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String syncIntervalStr = dotenv.get("PEER_SYNC_INTERVAL_SECONDS");
        String lastNStr = dotenv.get("PEER_SYNC_LAST_N");

        int syncIntervalSeconds = 600;
        int lastN = 200;

        if (syncIntervalStr != null && !syncIntervalStr.isBlank()) {
            try {
                syncIntervalSeconds = Integer.parseInt(syncIntervalStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PEER_SYNC_INTERVAL_SECONDS");
            }
        } else {
            System.err.println("PEER_SYNC_INTERVAL_SECONDS not found in .env, using default");
        }

        if (lastNStr != null && !lastNStr.isBlank()) {
            try {
                lastN = Integer.parseInt(lastNStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PEER_SYNC_LAST_N");
            }
        } else {
            System.err.println("PEER_SYNC_LAST_N not found in .env, using default");
        }

        this.peerNetwork = new PeerNetwork(client, stats, peersRepo, syncIntervalSeconds, lastN);

        String host = "localhost";
        try {
            InetAddress local = InetAddress.getLocalHost();
            if (local != null && local.getHostAddress() != null) host = local.getHostAddress();
        } catch (Exception ignored) {}

        PeerInfo selfInfo = new PeerInfo(host, port);
        selfInfo.status = "SELF";

        this.peerNetwork.setSelfInfo(selfInfo);

        if (seedPeers != null) {
                for (String s : seedPeers) peerNetwork.addPeer(s);
        }


        RequestHandler handler = new RequestHandler(this.stats, requestsRepo, this.peerNetwork);
        ServerMediator mediator = new ConcreteServerMediator(server, handler, stats);
    }

    public void start() {
        System.out.println(name + " starting on port " + server.getPort());
        server.Start();
        peerNetwork.start();
    }

    public void stop() {
        server.Stop();
        peerNetwork.stop();
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