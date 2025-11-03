package server;

import model.HttpRequest;
import model.HttpResponse;
import factory.StaticFileResponseCreator;
import factory.SuccessResponseCreator;
import factory.ErrorResponseCreator;
import p2p.PeerInfo;
import p2p.PeerNetwork;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

import static http.HttpUtils.tryParseJsonFlat;
import static http.HttpUtils.getHeaderIgnoreCase;

public class PeersController implements IController {
    private final PeerNetwork peerNetwork;


    public PeersController(PeerNetwork network) {
        this.peerNetwork = network;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        String method = req.method().toUpperCase();
        String adminHeader = getHeaderIgnoreCase(req.headers(), "X-Admin");
        boolean admin = "true".equalsIgnoreCase(adminHeader);

        switch (method) {
            case "GET" -> {
                JSONArray arr = new JSONArray();

                PeerInfo self = peerNetwork.getSelfInfo();
                if (self != null) {
                    JSONObject o = new JSONObject();
                    o.put("address", self.toAddress());
                    o.put("status", self.status);
                    o.put("lastSeen", self.lastSeen == null ? JSONObject.NULL : self.lastSeen.toString());
                    o.put("self", true);
                    arr.put(o);
                }

                for (var p : peerNetwork.listPeers()) {
                    JSONObject o = new JSONObject();
                    o.put("address", p.toAddress());
                    o.put("status", p.status);
                    o.put("lastSeen", p.lastSeen == null ? JSONObject.NULL : p.lastSeen.toString());
                    o.put("self", false);
                    arr.put(o);
                }
                String body = "{\"peers\":" + arr + "}";
                return new StaticFileResponseCreator("application/json").createResponse(200, body);
            }
            case "POST" -> {
                if (!admin) return new ErrorResponseCreator().createResponse(403, "{\"error\":\"admin required\"}");
                String raw = req.body();
                Map<String, Object> m = tryParseJsonFlat(raw);
                String addr = (String) m.get("address");
                if (addr == null || addr.isBlank())
                    return new ErrorResponseCreator().createResponse(400, "{\"error\":\"address required\"}");
                peerNetwork.addPeer(addr);
                return new SuccessResponseCreator().createResponse(200, "{\"ok\":true}");
            }
            case "DELETE" -> {
                if (!admin) return new ErrorResponseCreator().createResponse(403, "{\"error\":\"admin required\"}");
                String id = null;
                Map<String, Object> q = http.HttpUtils.parseQueryToContext(req);
                if (q.containsKey("id")) id = String.valueOf(q.get("id"));
                if ((id == null || id.isBlank()) && req.body() != null) {
                    Map<String, Object> m = tryParseJsonFlat(req.body());
                    if (m.containsKey("address")) id = (String) m.get("address");
                }
                if (id == null || id.isBlank())
                    return new ErrorResponseCreator().createResponse(400, "{\"error\":\"address required\"}");
                boolean removed = peerNetwork.removePeer(id);
                if (removed) return new SuccessResponseCreator().createResponse(200, "{\"ok\":true}");
                else return new ErrorResponseCreator().createResponse(404, "{\"error\":\"not found\"}");
            }
            default -> {
                return new ErrorResponseCreator().createResponse(405, "<h1>Method Not Allowed</h1>");
            }
        }
    }
}