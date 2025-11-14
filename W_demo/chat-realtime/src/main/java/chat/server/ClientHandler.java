package chat.server;

import chat.core.Message;
import java.io.*;
import java.net.*;
import java.util.Set;

class ClientHandler implements Runnable {
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
                if (!(obj instanceof Message)) continue;
                Message msg = (Message) obj;
                if (username == null || username.isEmpty()) {
                    username = msg.getSender();
                    if (username != null && !username.trim().isEmpty()) {
                        server.notifyUserJoined(username);
                    }
                }
                server.broadcastMessage(msg);
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
                server.notifyUserLeft(username);
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
}