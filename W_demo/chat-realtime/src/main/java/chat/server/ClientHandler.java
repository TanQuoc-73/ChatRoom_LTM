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

                if (username == null || username.isEmpty()) {
                    username = env.getSender();
                    if (username != null && !username.trim().isEmpty()) {
                        String token = null;
                        if (env.getMetadata() != null) {
                            Object t = env.getMetadata().get("token");
                            if (t != null) token = t.toString();
                        }
                        boolean ok = server.login(username, token == null ? "" : token, this);
                        if (!ok) {
                            onError("Authentication failed");
                            break;
                        }
                    } else {
                        onError("Missing username");
                        break;
                    }
                }


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
            Envelope env = new Envelope(
                    MessageType.CHAT_MESSAGE,
                    message.getRoomId(),
                    message.getSender(),
                    message.getContent()
            );
            out.writeObject(env);
            out.flush();
        } catch (IOException e) {
            
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

    public void sendMessage(String text) {
        sendMessage(new Message("system", "SYSTEM", text));
    }

    @Override
    public void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    public void onEvent(Object event) {
        try {
            Envelope env;
            if (event instanceof Events.UserJoined e) {
                env = new Envelope(MessageType.USER_JOINED, e.roomId, e.username, null);
            } else if (event instanceof Events.UserLeft e) {
                env = new Envelope(MessageType.USER_LEFT, e.roomId, e.username, null);
            } else if (event instanceof Events.UserTyping e) {
                env = new Envelope(e.typing ? MessageType.TYPING : MessageType.STOP_TYPING, e.roomId, e.username, null);
            } else {
                env = new Envelope(MessageType.ERROR, "system", "SYSTEM", "Unsupported event: " + event.getClass().getSimpleName());
            }
            out.writeObject(env);
            out.flush();
        } catch (IOException ioe) {
            
        }
    }

    @Override
    public void onError(String error) {
        try {
            Envelope env = new Envelope(MessageType.ERROR, "system", "SYSTEM", error);
            out.writeObject(env);
            out.flush();
        } catch (IOException e) {
            
        }
    }

    @Override
    public void onDisconnect() {
        disconnect();
    }
}