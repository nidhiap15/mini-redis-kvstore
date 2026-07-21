import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Store {
    private static final int MAX_SIZE = 5;

    // accessOrder=true makes this behave as LRU: every get() moves the key to "most recently used"
    private LinkedHashMap<String, String> data = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            boolean shouldEvict = size() > MAX_SIZE;
            if (shouldEvict) {
                System.out.println("Evicting least-recently-used key: " + eldest.getKey());
                expirations.remove(eldest.getKey());
            }
            return shouldEvict;
        }
    };

    private Map<String, Long> expirations = new java.util.HashMap<>();
    private PriorityQueue<ExpiryEntry> expiryHeap = new PriorityQueue<>();

    private static class ExpiryEntry implements Comparable<ExpiryEntry> {
        String key;
        long expiryTime;

        ExpiryEntry(String key, long expiryTime) {
            this.key = key;
            this.expiryTime = expiryTime;
        }

        @Override
        public int compareTo(ExpiryEntry other) {
            return Long.compare(this.expiryTime, other.expiryTime);
        }
    }

    public synchronized void set(String key, String value) {
        data.put(key, value);
        expirations.remove(key);
    }

    public synchronized void setWithExpiry(String key, String value, long ttlSeconds) {
        data.put(key, value);
        long expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        expirations.put(key, expiryTime);
        expiryHeap.add(new ExpiryEntry(key, expiryTime));
    }

    public synchronized String get(String key) {
        if (isExpired(key)) {
            return "(nil)";
        }
        if (!data.containsKey(key)) {
            return "(nil)";
        }
        return data.get(key); // accessing here also updates LRU order automatically
    }

    public synchronized void delete(String key) {
        data.remove(key);
        expirations.remove(key);
    }

    public synchronized void listKeys(StringBuilder out) {
        purgeExpired();
        if (data.isEmpty()) {
            out.append("(empty)");
            return;
        }
        for (String key : data.keySet()) {
            out.append(key).append("\n");
        }
    }

    private boolean isExpired(String key) {
        Long expiry = expirations.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            data.remove(key);
            expirations.remove(key);
            return true;
        }
        return false;
    }

    public synchronized void purgeExpired() {
        long now = System.currentTimeMillis();
        while (!expiryHeap.isEmpty() && expiryHeap.peek().expiryTime <= now) {
            ExpiryEntry entry = expiryHeap.poll();
            Long currentExpiry = expirations.get(entry.key);
            if (currentExpiry != null && currentExpiry == entry.expiryTime) {
                data.remove(entry.key);
                expirations.remove(entry.key);
            }
        }
    }
}