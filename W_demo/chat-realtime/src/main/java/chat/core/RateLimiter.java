package chat.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final Map<String, Map<String, Long>> userLimits = new ConcurrentHashMap<>();
    private final Map<String, Integer> limits = new ConcurrentHashMap<>();

    public RateLimiter() {
        limits.put("message", 1000);
        limits.put("join_room", 5000);
    }

    public boolean isAllowed(String username, String action) {
        int limitMs = limits.getOrDefault(action, 1000);
        long now = System.currentTimeMillis();

        Map<String, Long> actions = userLimits.computeIfAbsent(username, k -> new ConcurrentHashMap<>());
        Long last = actions.get(action);

        if (last == null || now - last >= limitMs) {
            actions.put(action, now);
            return true;
        }
        return false;
    }

    public void setLimit(String action, int limitMs) {
        limits.put(action, limitMs);
    }
}
