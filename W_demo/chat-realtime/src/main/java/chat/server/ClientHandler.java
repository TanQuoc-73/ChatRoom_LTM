package chat.server;

import java.io.*;
import java.net.*;
import java.util.Set;

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Set<ClientHandler> clients;
    private ChatServer server;
    private String username;

    public ClientHandler(Socket socket, Set<ClientHandler> clients, ChatServer server) throws IOException {
        this.socket = socket;
        this.clients = clients;
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

    public void disconnect() {
        try {
            if (username != null) {
                server.notifyUserLeft(username);
                server.broadcastMessage("SYSTEM: " + username + " đã rời khỏi phòng chat!");
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
}