package metrics;

import db.ResponseStatsRepository;
import events.ResponseEvent;
import events.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ResponseStatsPersister {
    private static final AtomicBoolean registered = new AtomicBoolean(false);
    private final ResponseStatsRepository repo;
    private final Consumer<ResponseEvent> handler;

    public ResponseStatsPersister(ResponseStatsRepository repo) {
        this.repo = repo;
        this.handler = this::onResponse;

        if (registered.compareAndSet(false, true)) {
            EventBus.getInstance().subscribe(ResponseEvent.class, handler);
            System.out.println("ResponseStatsPersister subscribed");
        } else {
            System.out.println("ResponseStatsPersister: already subscribed, skipping");
        }
    }

    private void onResponse(ResponseEvent e) {
        try {
            if (repo == null || e == null) return;
            String method = e.request == null ? "" : e.request.method();
            String path = e.request == null ? "" : e.request.url();
            int status = e.response == null ? 0 : e.response.statusCode();
            double durationMs = e.durationMs();
            String ts = e.ts == null ? null : e.ts.toString();
            repo.saveResponseStat(method, path, status, durationMs, ts);
        } catch (Throwable t) {
            System.err.println("ResponseStatsPersister failed to persist event: " + t.getMessage());
        }
    }

    public void shutdown() {
        try {
            EventBus.getInstance().unsubscribe(ResponseEvent.class, handler);
        } catch (Throwable ignored) {}
    }
}