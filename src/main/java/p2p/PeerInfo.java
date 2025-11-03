package p2p;

import java.time.Instant;

public class PeerInfo {
    public final String host;
    public final int port;
    public volatile String status;
    public volatile Instant lastSeen;

    public PeerInfo(String host, int port) {
        this.host = host;
        this.port = port;
        this.status = "UNKNOWN";
        this.lastSeen = null;
    }

    public String toAddress() {
        return host + ":" + port;
    }
}
