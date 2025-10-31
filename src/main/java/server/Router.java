package server;

import model.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Router {
    private record RouteKey(String method, String path) {}
    private final Map<RouteKey, IController> exact = new LinkedHashMap<>();
    private final Map<RouteKey, IController> prefix = new LinkedHashMap<>();

    public void registerExact(String method, String path, IController controller) {
        exact.put(new RouteKey(method.toUpperCase(), path), Objects.requireNonNull(controller));
    }

    public void registerPrefix(String method, String prefixPath, IController controller) {
        prefix.put(new RouteKey(method.toUpperCase(), prefixPath), Objects.requireNonNull(controller));
    }

    public HttpResponse route(model.HttpRequest req) {
        String method = req.method().toUpperCase();
        String url = req.url();

        IController c = exact.get(new RouteKey(method, url));
        if (c != null) return c.handle(req);

        IController best = null;
        int bestLen = -1;
        for (var e : prefix.entrySet()) {
            RouteKey k = e.getKey();
            if (!k.method().equals(method)) continue;
            String p = k.path();
            if (url.startsWith(p) && p.length() > bestLen) {
                bestLen = p.length();
                best = e.getValue();
            }
        }
        if (best != null) return best.handle(req);

        return new factory.ErrorResponseCreator().createResponse(404, "<h1>404 Not Found</h1>");
    }
}
