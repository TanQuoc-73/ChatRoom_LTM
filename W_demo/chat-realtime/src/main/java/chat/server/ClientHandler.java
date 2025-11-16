package chat.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class ClientHandler implements Runnable, MessageListener {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Set<ClientHandler> clients;
    private ChatServer server;
    private ChatService chatService;
    private String username;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean connected = false;

    public ClientHandler(Socket socket, ChatServer server, ChatService chatService) throws IOException {
        this.socket = socket;
        this.server = server;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            writer.println("ENTER_USERNAME");
            username = reader.readLine();
            
            if (username != null && !username.trim().isEmpty()) {
                server.notifyUserJoined(username);
                server.broadcastMessage("SYSTEM: " + username + " đã tham gia phòng chat!");

                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) break;
                    
                    server.broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
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
                server.notifyUserLeft(username);
                server.broadcastMessage("SYSTEM: " + username + " đã rời khỏi phòng chat!");
            }
            
            server.removeClient(this);
            
        } catch (Exception e) {
            System.err.println("Error during client disconnect: " + e.getMessage());
        }
    }


    public String getUsername() {
        return username;
    }
}