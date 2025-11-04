package logging;

import events.RequestEvent;
import events.ResponseEvent;
import events.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {
    private static final AtomicBoolean registered = new AtomicBoolean(false);

    public Logger() {
        if (registered.compareAndSet(false, true)) {
            EventBus.getInstance().subscribe(RequestEvent.class, this::onReq);
            EventBus.getInstance().subscribe(ResponseEvent.class, this::onResp);
        }
    }

    private void onReq(RequestEvent e) {
        System.out.println("[EVENT] request " + e.request.method() + " " + e.request.url() + " at " + e.ts);
    }

    private void onResp(ResponseEvent e) {
        System.out.println("[EVENT] response " + e.request.method() + " " + e.request.url() +
                " status=" + (e.response == null ? "null" : e.response.statusCode()) +
                " timeMs=" + String.format("%.2f", e.durationMs()));
    }
}
