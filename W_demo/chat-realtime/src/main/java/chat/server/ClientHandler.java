package chat.server;

import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.protocol.Events;
import java.io.*;
import java.net.*;
import java.util.Set;

class ClientHandler implements Runnable, MessageListener {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Set<ClientHandler> clients;
    private ChatServer server;
    private String username;

    public ClientHandler(Socket socket, Set<ClientHandler> clients, ChatServer server) throws IOException {
        this.socket = socket;
        this.clients = clients;
        this.server = server;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (!(obj instanceof Envelope)) continue;
                Envelope env = (Envelope) obj;

                // First-time association and login
                if (username == null || username.isEmpty()) {
                    username = env.getSender();
                    if (username != null && !username.trim().isEmpty()) {
                        // Authenticate and register this handler as a session listener
                        boolean ok = server.login(username, "", this);
                        if (!ok) {
                            onError("Authentication failed");
                            break;
                        }
                    } else {
                        onError("Missing username");
                        break;
                    }
                }

                // Route according to protocol
                if (env.getType() == MessageType.JOIN_ROOM) {
                    server.joinRoom(username, env.getRoomId());
                } else if (env.getType() == MessageType.LEAVE_ROOM) {
                    server.leaveRoom(username, env.getRoomId());
                } else {
                    server.sendMessage(env);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            // ignore per-connection send error
        }
    }

    public void disconnect() {
        try {
            if (username != null) {
                server.logout(username);
            }
            clients.remove(this);
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    // Backward-compatible helper for places that send plain text
    public void sendMessage(String text) {
        sendMessage(new Message("system", "SYSTEM", text));
    }

    // MessageListener implementation
    @Override
    public void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    public void onEvent(Object event) {
        // Convert protocol events to serializable Message payloads
        if (event instanceof Events.UserJoined e) {
            sendMessage(new Message(e.roomId, "SYSTEM", e.username + " joined"));
            return;
        }
        if (event instanceof Events.UserLeft e) {
            sendMessage(new Message(e.roomId, "SYSTEM", e.username + " left"));
            return;
        }
        if (event instanceof Events.UserTyping e) {
            sendMessage(new Message(e.roomId, "SYSTEM", e.username + (e.typing ? " is typing..." : " stopped typing")));
            return;
        }
        // Fallback generic event
        sendMessage(new Message("system", "SYSTEM", "EVENT:" + event.getClass().getSimpleName()));
    }

    @Override
    public void onError(String error) {
        sendMessage(new Message("system", "SYSTEM", error));
    }

    @Override
    public void onDisconnect() {
        disconnect();
    }
}