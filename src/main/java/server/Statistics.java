package server;

import db.StatisticsRepository;
import model.HttpRequest;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Statistics {
    private static final int SEQ_BITS = 20;
    private static final long SEQ_MASK = (1L << SEQ_BITS) - 1L;

    private final ConcurrentSkipListSet<Long> ids = new ConcurrentSkipListSet<>();
    private final AtomicLong seqCounter = new AtomicLong(0);
    private final StatisticsRepository repository;

    public Statistics(StatisticsRepository repository) {
        this.repository = repository;

        if (repository != null) {
            List<Long> existingIds = repository.loadAllIds();
            ids.addAll(existingIds);
            System.out.println("Loaded " + existingIds.size() + " requests from DB");
        }
    }

    public void logRequest(HttpRequest req) {
        long id = generateId();
        ids.add(id);

        // Зберігаємо в MongoDB
        if (repository != null) {
            String timestamp = Instant.now().toString();
            repository.saveRequest(id, req.method(), req.url(), timestamp);
        }

        System.out.println("Logged request: " + req.method() + " " + req.url() + " id=" + id);
    }

    public int getTotalRequests() {
        return ids.size();
    }

    public int mergeFromPeer(Collection<Long> remoteIds) {
        if (remoteIds == null || remoteIds.isEmpty()) return 0;
        int added = 0;
        for (Long id : remoteIds) {
            if (id == null) continue;
            if (ids.add(id)) {
                added++;
                // Зберігаємо отриманий ID в БД
                if (repository != null) {
                    repository.saveRequest(id, "MERGED", "/synced", Instant.now().toString());
                }
            }
        }
        if (added > 0) {
            System.out.println("Merged " + added + " external events. Total now: " + getTotalRequests());
        }
        return added;
    }

    public int mergeFromTimestampsJson(String json) {
        if (json == null || json.isBlank()) return 0;
        int start = json.indexOf('[');
        int end = json.indexOf(']', Math.max(start, 0));
        if (start < 0 || end < 0 || end <= start) return 0;
        String inside = json.substring(start + 1, end).trim();
        if (inside.isEmpty()) return 0;
        String[] parts = inside.split(",");
        List<Long> remote = new ArrayList<>(parts.length);
        for (String p : parts) {
            try {
                long v = Long.parseLong(p.trim());
                remote.add(v);
            } catch (NumberFormatException ignored) {}
        }
        return mergeFromPeer(remote);
    }

    public String toJsonTimestamps() {
        String body = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return "{\"timestamps\":[" + body + "]}";
    }

    public String toJsonTimestampsLimited(int n) {
        if (n <= 0) return "{\"timestamps\":[]}";
        ArrayList<Long> last = new ArrayList<>(n);
        Iterator<Long> it = ids.descendingIterator();
        while (it.hasNext() && last.size() < n) last.add(it.next());
        Collections.reverse(last);
        String body = last.stream().map(String::valueOf).collect(Collectors.joining(","));
        return "{\"timestamps\":[" + body + "]}";
    }


    private long generateId() {
        long ts = System.currentTimeMillis();
        long seq = seqCounter.getAndIncrement() & SEQ_MASK;
        return (ts << SEQ_BITS) | seq;
    }

    public void clearStats() {
        ids.clear();
        if (repository != null) {
            repository.clearAll();
        }
    }
}
