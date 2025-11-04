package httpserver;

import p2p.PeerNode;
import java.util.List;

public class Main {
    static void main() {
        PeerNode nodeA = new PeerNode("PeerA", 8080, List.of("localhost:8081"));
        PeerNode nodeB = new PeerNode("PeerB", 8081, List.of("localhost:8080"));

        nodeA.start();
        nodeB.start();
    }
}