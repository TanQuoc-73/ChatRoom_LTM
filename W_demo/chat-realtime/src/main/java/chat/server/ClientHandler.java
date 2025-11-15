package chat.server;

import chat.core.ChatService;
import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.protocol.Events;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class ClientHandler implements Runnable, MessageListener {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private ChatServer server;
    private ChatService chatService;
    private String username;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean connected = false;

    public ClientHandler(Socket socket, ChatServer server, ChatService chatService) throws IOException {
        this.socket = socket;
        this.server = server;
        this.chatService = chatService;
 
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        connected = true;
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        System.out.println("Client handler started: " + clientInfo);
        
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                processClientMessage(line);
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("Client disconnected: " + clientInfo);
            }
        } catch (Exception e) {
            System.err.println("Client handler error for " + clientInfo + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processClientMessage(String jsonLine) {
        try {
            Envelope envelope = mapper.readValue(jsonLine, Envelope.class);
            handleEnvelope(envelope);
        } catch (IOException e) {
            sendError("Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            sendError("Message processing error: " + e.getMessage());
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void handleEnvelope(Envelope envelope) {
        // Kiểm tra user có vào room trước khi gửi tin nhắn
        if (this.username == null && envelope.getType() != MessageType.JOIN_ROOM) {
            sendError("Must join room first");
            return;
        }

        switch (envelope.getType()) {
            case JOIN_ROOM -> handleJoinRoom(envelope);
            case CHAT_MESSAGE -> handleChatMessage(envelope);
            case PRIVATE_MESSAGE -> handlePrivateMessage(envelope);
            case LEAVE_ROOM -> handleLeaveRoom(envelope);
            case TYPING -> handleTypingIndicator(envelope, true);
            case STOP_TYPING -> handleTypingIndicator(envelope, false);
            default -> sendError("Unsupported message type: " + envelope.getType());
        }
    }

    // Xử lý user join room (gồm cả xác thực)
    private void handleJoinRoom(Envelope envelope) {
        String newUsername = envelope.getSender();
        String roomId = envelope.getRoomId();

        if (newUsername == null || newUsername.trim().isEmpty()) {
            sendError("Username is required");
            return;
        }

        if (roomId == null || roomId.trim().isEmpty()) {
            sendError("Room ID is required");
            return;
        }

        try {
            // Lấy token nếu có trong metadata
            String token = null;
            if (envelope.getMetadata() != null) {
                Object tokenObj = envelope.getMetadata().get("token");
                if (tokenObj != null) token = tokenObj.toString();
            }

            // Xác thực user qua ChatService
            boolean authSuccess = chatService.login(newUsername, token != null ? token : "", this);
            if (!authSuccess) {
                sendError("Authentication failed for user: " + newUsername);
                return;
            }

            // Cho user vào room
            chatService.joinRoom(newUsername, roomId);
            
            this.username = newUsername;
            System.out.println("User '" + username + "' joined room: " + roomId);

            // Gửi event USER_JOINED về client
            Envelope joinedEvent = new Envelope(MessageType.USER_JOINED, roomId, username, "joined");
            sendEnvelope(joinedEvent);
            
            sendAck("Successfully joined room: " + roomId);

        } catch (Exception e) {
            sendError("Join room failed: " + e.getMessage());
            System.err.println("Join room error for " + newUsername + ": " + e.getMessage());
        }
    }

    private void handleChatMessage(Envelope envelope) {
        try {
            // Gửi message vào core để Router xử lý
            chatService.sendMessage(envelope);
        } catch (Exception e) {
            sendError("Failed to send message: " + e.getMessage());
        }
    }

    private void handlePrivateMessage(Envelope envelope) {
        try {
            chatService.sendMessage(envelope);
        } catch (Exception e) {
            sendError("Failed to send private message: " + e.getMessage());
        }
    }

    private void handleLeaveRoom(Envelope envelope) {
        try {
            chatService.leaveRoom(username, envelope.getRoomId());
            sendAck("Left room: " + envelope.getRoomId());
        } catch (Exception e) {
            sendError("Failed to leave room: " + e.getMessage());
        }
    }

    private void handleTypingIndicator(Envelope envelope, boolean typing) {
        try {
            // Forward typing indicator đến Core Router
            chatService.getRouter().route(envelope);
        } catch (Exception e) {
            System.err.println("Typing indicator error: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message) {
        Envelope env = new Envelope(
            MessageType.CHAT_MESSAGE, 
            message.getRoomId(), 
            message.getSender(), 
            message.getContent()
        );
        sendEnvelope(env);
    }

    @Override
    public void onEvent(Object event) {
        try {
            if (event instanceof Events.UserJoined e) {
                Envelope env = new Envelope(MessageType.USER_JOINED, e.roomId, e.username, "joined");
                sendEnvelope(env);

            } else if (event instanceof Events.UserLeft e) {
                Envelope env = new Envelope(MessageType.USER_LEFT, e.roomId, e.username, "left");
                sendEnvelope(env);

            } else if (event instanceof Events.UserTyping e) {
                MessageType type = e.typing ? MessageType.TYPING : MessageType.STOP_TYPING;
                Envelope env = new Envelope(type, e.roomId, e.username, "");
                sendEnvelope(env);

            } else {
                System.out.println("Unhandled event type: " + event.getClass().getSimpleName());
            }
        } catch (Exception e) {
            System.err.println("Error sending event: " + e.getMessage());
        }
    }

    @Override
    public void onError(String error) {
        sendError(error);
    }

    @Override
    public void onDisconnect() {
        // Core thông báo yêu cầu disconnect
        System.out.println("Core requested disconnect for: " + username);
        disconnect();
    }

    @Override
    public void onAck(String ackData) {
        sendEnvelope(new Envelope(MessageType.ACK, null, "system", ackData));
    }

    // Gửi envelope dạng JSON về client
    void sendEnvelope(Envelope envelope) {
        if (!connected || socket.isClosed()) return;
        
        try {
            String json = mapper.writeValueAsString(envelope);
            synchronized (writer) {
                writer.write(json);
                writer.write("\n");
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to " + username + ": " + e.getMessage());
            disconnect();
        }
    }

    private void sendError(String error) {
        sendEnvelope(new Envelope(MessageType.ERROR, null, "system", error));
    }

    private void sendAck(String message) {
        sendEnvelope(new Envelope(MessageType.ACK, null, "system", message));
    }

    // Dọn dẹp kết nối client
    public void disconnect() {
        if (!connected) return;
        
        connected = false;
        System.out.println("Disconnecting client: " + username);
        
        try {
            if (username != null) {
                chatService.getSessions().unregisterUser(username);
            }
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            server.removeClient(this);
            
        } catch (Exception e) {
            System.err.println("Error during client disconnect: " + e.getMessage());
        }
    }


    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getClientInfo() {
        if (socket == null) return "disconnected";
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + 
               (username != null ? " (" + username + ")" : "");
    }
}