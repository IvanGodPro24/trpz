package metrics;

import events.RequestEvent;
import events.EventBus;
import server.Statistics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StatisticsPersister {
    private static final AtomicBoolean registered = new AtomicBoolean(false);
    private final Statistics statistics;
    private final Consumer<RequestEvent> handler;

    public StatisticsPersister(Statistics statistics) {
        this.statistics = statistics;
        this.handler = this::onRequest;

        if (registered.compareAndSet(false, true)) {
            EventBus.getInstance().subscribe(RequestEvent.class, handler);
            System.out.println("StatisticsPersister subscribed");
        } else {
            System.out.println("StatisticsPersister: already subscribed, skipping");
        }
    }


    private void onRequest(RequestEvent e) {
        try {
            if (statistics == null || e == null || e.request == null) return;
            statistics.logRequest(e.request);
        } catch (Throwable t) {
            System.err.println("StatisticsPersister failed to persist request: " + t.getMessage());
        }
    }

    public void shutdown() {
        try {
            EventBus.getInstance().unsubscribe(RequestEvent.class, handler);
        } catch (Throwable ignored) {}
    }
}

