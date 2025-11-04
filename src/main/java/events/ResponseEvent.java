package events;

import model.HttpRequest;
import model.HttpResponse;
import java.time.Instant;

public class ResponseEvent implements Event {
    public final HttpRequest request;
    public final HttpResponse response;
    public final long durationNanos;
    public final Instant ts;

    public ResponseEvent(HttpRequest request, HttpResponse response, long durationNanos) {
        this.request = request;
        this.response = response;
        this.durationNanos = durationNanos;
        this.ts = Instant.now();
    }

    public double durationMs() {
        return durationNanos / 1_000_000.0;
    }
}

