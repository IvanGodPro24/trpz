package server;

import metrics.MetricsCollector;
import org.json.JSONObject;
import model.HttpResponse;
import factory.StaticFileResponseCreator;

public class MetricsController implements IController {
    private final MetricsCollector metrics;

    public MetricsController(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public HttpResponse handle(model.HttpRequest req) {
        JSONObject o = new JSONObject();
        o.put("totalRequests", metrics.getTotalRequests());
        o.put("avgResponseMs", metrics.getAvgResponseMs());
        o.put("rps", metrics.getRps());
        String body = o.toString();
        return new StaticFileResponseCreator("application/json").createResponse(200, body);
    }
}
