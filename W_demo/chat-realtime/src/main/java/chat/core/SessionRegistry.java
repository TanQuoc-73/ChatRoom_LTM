package chat.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import chat.core.protocol.Envelope;

public class SessionRegistry {
    private final Map<String, Set<MessageListener>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();

    public void registerUser(String username, MessageListener listener) {
        userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(listener);
    }

    public void unregisterUser(String username) {
        Set<MessageListener> listeners = userSessions.remove(username);
        if (listeners != null)
            listeners.forEach(MessageListener::onDisconnect);
        roomSessions.values().forEach(s -> s.remove(username));
    }

    public void joinRoom(String username, String roomId) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username);
    }

    public void leaveRoom(String username, String roomId) {
        Set<String> users = roomSessions.get(roomId);
        if (users != null) {
            users.remove(username);
            if (users.isEmpty()) roomSessions.remove(roomId);
        }
    }

    public Set<String> getUsersInRoom(String roomId) {
        return new HashSet<>(roomSessions.getOrDefault(roomId, Collections.emptySet()));
    }

    public boolean isUserOnline(String username) { return userSessions.containsKey(username); }

    public MessageListener getUserListener(String username) {
        Set<MessageListener> listeners = userSessions.get(username);
        return (listeners == null || listeners.isEmpty()) ? null : listeners.iterator().next();
    }

    public void broadcastToRoom(String roomId, Envelope envelope) {
        Message message = new Message(envelope.getRoomId(), envelope.getSender(), envelope.getPayload());
        for (String u : getUsersInRoom(roomId)) {
            MessageListener l = getUserListener(u);
            if (l != null) l.onMessage(message);
        }
    }

    public void broadcastToRoomExcept(String roomId, String excluded, Object event) {
        for (String u : getUsersInRoom(roomId)) {
            if (!u.equals(excluded)) {
                MessageListener l = getUserListener(u);
                if (l != null) l.onEvent(event);
            }
        }
    }
}
