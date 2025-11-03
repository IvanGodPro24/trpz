package p2p;

import db.PeersRepository;
import server.Statistics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerNetwork {
    private final ConcurrentHashMap<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final PeerClient client;
    private final Statistics stats;
    private final PeersRepository peersRepo;

    private volatile PeerInfo selfInfo;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int syncIntervalSeconds;
    private final int lastN;

    public PeerNetwork(PeerClient client, Statistics stats, PeersRepository peersRepo, int syncIntervalSeconds, int lastN) {
        this.client = client;
        this.stats = stats;
        this.peersRepo = peersRepo;
        this.syncIntervalSeconds = syncIntervalSeconds <= 0 ? 30 : syncIntervalSeconds;
        this.lastN = Math.max(1, lastN);

        if (peersRepo != null) {
            List<String> persisted = peersRepo.loadAll();
            for (String addr : persisted) addPeer(addr, false);
        }
    }

    public void setSelfInfo(PeerInfo self) {
        this.selfInfo = self;
    }

    public PeerInfo getSelfInfo() {
        return selfInfo;
    }

    public void addPeer(String hostPort) {
        addPeer(hostPort, true);
    }

    public void addPeer(String hostPort, boolean persist) {
        if (hostPort == null || hostPort.isBlank()) return;

        String[] parts = hostPort.split(":", 2);
        String rawHost = parts[0];
        int port = parts.length > 1 ? parsePort(parts[1]) : 80;

        String normalizedHost = rawHost;
        try {
            InetAddress addr = InetAddress.getByName(rawHost);
            if (addr != null && addr.getHostAddress() != null) normalizedHost = addr.getHostAddress();
        } catch (UnknownHostException ignored) {}

        String normalizedAddr = normalizedHost + ":" + port;

        PeerInfo self = this.selfInfo;
        if (self != null) {
            String selfNormalized = self.host;
            try {
                InetAddress s = InetAddress.getByName(self.host);
                if (s != null && s.getHostAddress() != null) selfNormalized = s.getHostAddress();
            } catch (UnknownHostException ignored) {}
            selfNormalized = selfNormalized + ":" + self.port;

            if (normalizedAddr.equals(selfNormalized) || hostPort.equals(self.toAddress())) {
                return;
            }
        }

        if (peers.containsKey(normalizedAddr)) return;

        PeerInfo p = new PeerInfo(normalizedHost, port);
        peers.put(normalizedAddr, p);

        if (persist && peersRepo != null) peersRepo.savePeer(normalizedAddr);
    }


    private int parsePort(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 80;
        }
    }

    public boolean removePeer(String hostPort) {
        PeerInfo removed = peers.remove(hostPort);
        boolean ok = removed != null;
        if (ok && peersRepo != null) peersRepo.deletePeer(hostPort);
        return ok;
    }

    public List<PeerInfo> listPeers() {
        return new ArrayList<>(peers.values());
    }

    // sync: schedule periodic job
    public void start() {
        scheduler.scheduleAtFixedRate(this::syncAllPeersScheduled, 5, syncIntervalSeconds, TimeUnit.SECONDS);
    }

    private void syncAllPeersScheduled() {
        try {
            syncAllPeers(lastN);
        } catch (Throwable t) {
            System.err.println("PeerNetwork scheduled sync failed: " + t.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public void syncAllPeers(int limitLastN) {
        String json = stats.toJsonTimestampsLimited(limitLastN);
        for (PeerInfo p : peers.values()) {
            try {
                String addr = p.toAddress();
                int attempts = 0;
                boolean success = false;
                while (attempts < 2 && !success) {
                    attempts++;
                    try {
                        var resp = client.sendPost(addr, "/sync/stats", json, "application/json; charset=utf-8");
                        if (resp != null && resp.statusCode() == 200) {
                            p.status = "UP";
                            p.lastSeen = Instant.now();
                            success = true;
                        } else {
                            p.status = "DOWN";
                        }
                    } catch (Exception e) {
                        p.status = "DOWN";
                        if (attempts < 2) {
                            try {
                                Thread.sleep(250L * attempts);
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to sync with peer " + p.toAddress() + ": " + e.getMessage());
            }
        }
    }
}