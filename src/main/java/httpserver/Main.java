package httpserver;

import p2p.PeerNode;
import java.util.List;

public class Main {
    static void main() {
        PeerNode nodeA = new PeerNode("PeerA", 8080, List.of("localhost:8081"));
        PeerNode nodeB = new PeerNode("PeerB", 8081, List.of("localhost:8080"));

        nodeA.start();
        nodeB.start();

//        nodeA.syncStatsToPeersPost();


//        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

//        nodeB.syncWithPeers();

        // PeerA надсилає запит до PeerB
//        nodeA.broadcastRequest("/home");

//        nodeA.syncWithPeers();

        // PeerB надсилає запит до PeerA
//        nodeB.broadcastRequest("/about");

//        nodeA.stop();
//        nodeB.stop();
    }
}