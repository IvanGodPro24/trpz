package events;

import model.HttpRequest;
import java.time.Instant;

public class RequestEvent implements Event {
    public final HttpRequest request;
    public final Instant ts;

    public RequestEvent(HttpRequest req) {
        this.request = req;
        this.ts = Instant.now();
    }
}
