package events;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus instance = new EventBus();

    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> subs = new ConcurrentHashMap<>();
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EventBus");
        t.setDaemon(true);
        return t;
    });

    private EventBus() {}

    public static EventBus getInstance() { return instance; }

    public <E extends Event> void subscribe(Class<E> cls, Consumer<E> handler) {
        subs.computeIfAbsent(cls, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public <E extends Event> void unsubscribe(Class<E> cls, Consumer<E> handler) {
        var list = subs.get(cls);
        if (list != null) list.remove(handler);
    }

    public void publish(Event e) {
        if (e == null) return;
        var list = subs.get(e.getClass());
        if (list == null || list.isEmpty()) return;

        exec.submit(() -> {
            for (var h : list) {
                try {
                    @SuppressWarnings("unchecked")
                    Consumer<Event> c = (Consumer<Event>) h;
                    c.accept(e);
                } catch (Throwable t) {
                    System.err.println("Event handler error: " + t.getMessage());
                }
            }
        });
    }

    public void shutdown() {
        exec.shutdown();
    }
}
