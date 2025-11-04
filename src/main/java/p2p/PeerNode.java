package p2p;

import db.MongoDBConnection;
import db.StatisticsRepository;
import db.RequestsRepository;
import db.PeersRepository;
import db.MetricsRepository;
import db.ResponseStatsRepository;
import mediator.ConcreteServerMediator;
import metrics.StatisticsPersister;
import model.HttpResponse;
import server.HttpServer;
import server.RequestHandler;
import server.Statistics;
import io.github.cdimascio.dotenv.Dotenv;
import metrics.MetricsCollector;
import metrics.ResponseStatsPersister;
import logging.Logger;

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
    private final MetricsCollector metricsCollector;
    private final ResponseStatsPersister responseStatsPersister;
    private final StatisticsPersister statisticsPersister;

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

        ResponseStatsRepository responseStatsRepo = new ResponseStatsRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        PeersRepository peersRepo = new PeersRepository(
                MongoDBConnection.getClient(),
                MongoDBConnection.getDatabaseName()
        );

        MetricsRepository metricsRepo = new MetricsRepository(
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

        this.metricsCollector = new MetricsCollector(metricsRepo);
        new Logger();

        this.responseStatsPersister = new ResponseStatsPersister(responseStatsRepo);
        this.statisticsPersister = new StatisticsPersister(this.stats);

        RequestHandler handler = new RequestHandler(this.stats, requestsRepo, this.peerNetwork, this.metricsCollector);
        new ConcreteServerMediator(server, handler);

        if (seedPeers != null) for (String s : seedPeers) peerNetwork.addPeer(s);
    }

    public void start() {
        System.out.println(name + " starting on port " + server.getPort());
        server.Start();
        peerNetwork.start();
    }

    public void stop() {
        server.Stop();
        peerNetwork.stop();
        if (metricsCollector != null) metricsCollector.shutdown();
        if (responseStatsPersister != null) responseStatsPersister.shutdown();
        if (statisticsPersister != null) statisticsPersister.shutdown();
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