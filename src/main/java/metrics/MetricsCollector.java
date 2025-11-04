package metrics;

import events.ResponseEvent;
import events.EventBus;
import db.MetricsRepository;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsCollector {
    private static final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTimeNanos = new AtomicLong(0);

    private final ConcurrentLinkedQueue<Long> timestampsMs = new ConcurrentLinkedQueue<>();

    private final MetricsRepository repo;
    private final ScheduledExecutorService scheduler;

    public MetricsCollector(MetricsRepository repo) {
        this.repo = repo;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String intervalSecondsStr = dotenv.get("INTERVAL_SECONDS");

        int interval = 600;
        if (intervalSecondsStr != null && !intervalSecondsStr.isBlank()) {
            try {
                interval = Integer.parseInt(intervalSecondsStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid INTERVAL_SECONDS");
            }
        } else {
            System.err.println("INTERVAL_SECONDS not found");
        }

        int intervalSeconds = interval;


        if (registered.compareAndSet(false, true)) {
            EventBus.getInstance().subscribe(ResponseEvent.class, this::onResponse);
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MetricsPersist");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::persistSnapshot, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void onResponse(ResponseEvent e) {
        totalRequests.incrementAndGet();
        totalResponseTimeNanos.addAndGet(e.durationNanos);
        timestampsMs.add(System.currentTimeMillis());

        long cutoff = System.currentTimeMillis() - 2000L;
        while (true) {
            Long head = timestampsMs.peek();
            if (head == null || head >= cutoff) break;
            timestampsMs.poll();
        }
    }

    private void persistSnapshot() {
        try {
            if (repo == null) return;
            long total = getTotalRequests();
            double avgMs = getAvgResponseMs();
            double rps = getRps();
            repo.saveSnapshot(total, avgMs, rps);
        } catch (Throwable t) {
            System.err.println("Metrics persistence failed: " + t.getMessage());
        }
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public double getAvgResponseMs() {
        long tr = totalRequests.get();
        if (tr == 0) return 0.0;
        return totalResponseTimeNanos.get() / (tr * 1_000_000.0);
    }

    public double getRps() {
        long now = System.currentTimeMillis();
        long from = now - 1000L;
        long count = 0;
        for (Long t : timestampsMs) {
            if (t >= from) count++;
        }
        return (double) count;
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
