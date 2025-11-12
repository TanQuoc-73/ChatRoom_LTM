package chat.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PresenceService {
    private final SessionRegistry registry;
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();

    public PresenceService(SessionRegistry registry) {
        this.registry = registry;
    }

    public void updatePresence(String username) {
        lastSeen.put(username, System.currentTimeMillis());
    }

    public boolean isUserActive(String username) {
        Long t = lastSeen.get(username);
        return t != null && (System.currentTimeMillis() - t) < 5 * 60 * 1000;
    }

    public Set<String> getActiveUsersInRoom(String roomId) {
        Set<String> active = new HashSet<>();
        for (String u : registry.getUsersInRoom(roomId))
            if (isUserActive(u)) active.add(u);
        return active;
    }
}
