package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chat.core.ChatService;
import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ChatService chatService;
    private boolean isRunning;

    public ChatServer(ChatService chatService) {
        if (chatService == null) {
            throw new IllegalArgumentException("ChatService cannot be null");
        }
        this.chatService = chatService;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            threadPool.execute(this::acceptConnections);

            // Server đã khởi động thành công
            System.out.println("chatserver đang ngắm cổng " + port);
            System.out.println("Core ChatService: " + chatService.getClass().getSimpleName());

        } catch (IOException e) {
            // Lỗi khởi động server
            System.err.println("sos lỗi ròi " + e.getMessage());
            throw new RuntimeException("Server startup failed", e);
        }
    }

    private void acceptConnections() {
        // Server bắt đầu lắng nghe kết nối từ client
        System.out.println("đang lắng nghe client");

        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                System.out.println("knoi client mới " + clientInfo);

                ClientHandler handler = new ClientHandler(socket, this, chatService);
                clients.add(handler);
                threadPool.execute(handler);

            } catch (IOException e) {
                if (isRunning) {
                    // Lỗi khi chấp nhận kết nối
                    System.err.println("Error chấp nhận knoi " + e.getMessage());
                }
            }
        }
    }

    // Dừng server và giải phóng tài nguyên
    public void stop() {
        System.out.println("đang đóng ChatServer...");
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Lỗi khi đóng socket server
            System.err.println("lỗi khi đóng server socket " + e.getMessage());
        }

        // Ngắt kết nối tất cả client
        System.out.println("Disconnecting " + clients.size() + " clients...");
        for (ClientHandler client : clients) {
            client.disconnect();
        }
        clients.clear();

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            System.out.println("tắt xong thread");
        }

        System.out.println("ChatServer stopped xong ");
    }

    void removeClient(ClientHandler client) {
        boolean removed = clients.remove(client);
        if (removed) {
            // Xóa client khỏi danh sách theo dõi
            System.out.println("Client disconnec " + client.getUsername());
        }
    }

    public ChatService getChatService() {
        return chatService;
    }

    public boolean isRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    public int getClientCount() {
        return clients.size();
    }

    public void broadcastAdminMessage(String message) {
        Envelope adminEnv = new Envelope(MessageType.CHAT_MESSAGE, "system", "ADMIN", message);

        // Gửi tin nhắn hệ thống đến toàn bộ client
        for (ClientHandler client : clients) {
            try {
                client.sendEnvelope(adminEnv);
            } catch (Exception e) {
                System.err.println("lỗi gửi tn ròi " + e.getMessage());
            }
        }
    }
}

class InMemoryAuthGateway implements AuthGateway {
    @Override
    public boolean authenticate(String username, String password) {
        return username != null && !username.trim().isEmpty();
    }

    @Override
    public boolean isUserInRoom(String username, String roomId) {
        return true;
    }

    @Override
    public boolean canUserJoinRoom(String username, String roomId) {
        return true;
    }

    @Override
    public boolean isAuthenticated(String username) {
        return username != null && !username.trim().isEmpty();
    }
}

class InMemoryMessageStore implements MessageStore {
    private final List<chat.core.Message> messages = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final int MAX_MESSAGES = Integer.MAX_VALUE; // save không ghan

    @Override
    public void saveMessage(chat.core.Message message) {
        synchronized (messages) {
            messages.add(message);
            if (messages.size() > MAX_MESSAGES) {
                messages.subList(0, messages.size() - MAX_MESSAGES).clear();
            }
        }
    }

    @Override
    public java.util.List<chat.core.Message> getMessages(String roomId, int limit, long beforeTimestamp) {
        return messages.stream()
                .filter(msg -> msg.getRoomId().equals(roomId))
                .filter(msg -> beforeTimestamp == 0 || msg.getTimestamp() < beforeTimestamp)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit > 0 ? limit : 50)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<chat.core.Message> getMessagesSince(String roomId, long sinceTimestamp) {
        return messages.stream()
                .filter(msg -> msg.getRoomId().equals(roomId))
                .filter(msg -> msg.getTimestamp() > sinceTimestamp)
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteMessage(String messageId) {
        messages.removeIf(msg -> msg.getId().equals(messageId));
    }
}

class InMemoryRoomStore implements RoomStore {
    private final java.util.concurrent.ConcurrentMap<String, java.util.Set<String>> rooms =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    @Override
    public void createRoom(String roomId, String createdBy) {
        rooms.putIfAbsent(roomId, java.util.concurrent.ConcurrentHashMap.newKeySet());
    }

    @Override
    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }

    @Override
    public java.util.Set<String> getRoomUsers(String roomId) {
        return new java.util.HashSet<>(rooms.getOrDefault(roomId, java.util.Collections.emptySet()));
    }

    @Override
    public java.util.List<String> getAllRooms() {
        return new java.util.ArrayList<>(rooms.keySet());
    }

    @Override
    public void addUserToRoom(String roomId, String username) {
        rooms.computeIfAbsent(roomId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(username);
    }

    @Override
    public void removeUserFromRoom(String roomId, String username) {
        java.util.Set<String> users = rooms.get(roomId);
        if (users != null) {
            users.remove(username);

            if (users.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }
}