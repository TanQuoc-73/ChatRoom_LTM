package chat.server;

import chat.core.ChatService;
import chat.core.MessageListener;
import chat.core.Message;
import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer extends ChatService{
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Set<ClientHandler> clients;
    private List<MessageListener> listeners;
    private boolean isRunning;

    public ChatServer() {
        super(new InMemoryAuth(), new InMemoryMessageStore(), new InMemoryRoomStore());
        this.clients = ConcurrentHashMap.newKeySet();
        this.listeners = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            threadPool.execute(this::acceptConnections);
            notifyMessage("Server started on port " + port);
        } catch (IOException e) {
            notifyError("Failed to start server: " + e.getMessage());
        }
    }

    private void acceptConnections() {
        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, clients, this);
                clients.add(handler);
                threadPool.execute(handler);
            } catch (IOException e) {
                if (isRunning) {
                    notifyError("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            notifyError("Error stopping server: " + e.getMessage());
        }
        
        for (ClientHandler client : clients) {
            client.disconnect();
        }
        clients.clear();
        threadPool.shutdown();
    }

    public void broadcastMessage(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        notifyMessage(message.getSender() + ": " + message.getContent());
    }

    public void notifyUserJoined(String username) {
        broadcastMessage(new Message("system", "SYSTEM", "" + username + " đã tham gia phòng chat!"));
    }

    public void notifyUserLeft(String username) {
        broadcastMessage(new Message("system", "SYSTEM", "" + username + " đã rời khỏi phòng chat!"));
    }

    private void notifyMessage(String message) {
        for (MessageListener listener : listeners) {
            listener.onMessage(new Message("system", "SYSTEM", message));
        }
    }

    private void notifyError(String error) {
        for (MessageListener listener : listeners) {
            listener.onError(error);
        }
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }
    public void connectToServer(String host, int port, String username) {}
    public void sendMessage(String message) {}
    public void disconnect() {}

    // ----- Simple in-memory SPI implementations for convenience -----
    private static class InMemoryAuth implements AuthGateway {
        @Override
        public boolean authenticate(String username, String password) {
            return username != null && !username.trim().isEmpty();
        }

        @Override
        public boolean isUserInRoom(String username, String roomId) { return false; }

        @Override
        public boolean canUserJoinRoom(String username, String roomId) { return true; }
    }

    private static class InMemoryMessageStore implements MessageStore {
        @Override
        public void saveMessage(chat.core.Message message) { }

        @Override
        public java.util.List<chat.core.Message> getMessages(String roomId, int limit, long beforeTimestamp) { return java.util.Collections.emptyList(); }

        @Override
        public java.util.List<chat.core.Message> getMessagesSince(String roomId, long sinceTimestamp) { return java.util.Collections.emptyList(); }

        @Override
        public void deleteMessage(String messageId) { }
    }

    private static class InMemoryRoomStore implements RoomStore {
        private final ConcurrentMap<String, Set<String>> rooms = new ConcurrentHashMap<>();

        @Override
        public boolean roomExists(String roomId) { return rooms.containsKey(roomId); }

        @Override
        public void createRoom(String roomId, String createdBy) { rooms.putIfAbsent(roomId, ConcurrentHashMap.newKeySet()); }

        @Override
        public void deleteRoom(String roomId) { rooms.remove(roomId); }

        @Override
        public Set<String> getRoomUsers(String roomId) { return rooms.getOrDefault(roomId, java.util.Collections.emptySet()); }

        @Override
        public java.util.List<String> getAllRooms() { return new java.util.ArrayList<>(rooms.keySet()); }

        @Override
        public void addUserToRoom(String roomId, String username) { rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username); }

        @Override
        public void removeUserFromRoom(String roomId, String username) { Set<String> s = rooms.get(roomId); if (s != null) s.remove(username); }
    }
}