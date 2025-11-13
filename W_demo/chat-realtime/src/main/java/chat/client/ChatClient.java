package chat.client;

import chat.core.ChatService;
import chat.core.MessageListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatClient implements ChatService {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private List<MessageListener> listeners;
    private boolean connected;

    public ChatClient() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void connectToServer(String host, int port, String username) {
        try {
            this.username = username;
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            String serverResponse = reader.readLine();
            if ("ENTER_USERNAME".equals(serverResponse)) {
                writer.println(username);
            }
            
            startMessageListener();
            
            notifyMessage("Connected to server successfully!");

        } catch (IOException e) {
            notifyError("Failed to connect: " + e.getMessage());
        }
    }

    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String message;
                while (connected && (message = reader.readLine()) != null) {
                    notifyMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    notifyError("Connection lost: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void sendMessage(String message) {
        if (connected && writer != null) {
            writer.println(message);
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        try {
            if (writer != null) {
                writer.println("exit");
            }
            if (socket != null) {
                socket.close();
            }
            notifyMessage("Disconnected from server");
        } catch (IOException e) {
            notifyError("Error during disconnect: " + e.getMessage());
        }
    }

    @Override
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyMessage(String message) {
        for (MessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyError(String error) {
        for (MessageListener listener : listeners) {
            listener.onError(error);
        }
    }

    @Override
    public void startServer(int port) {}
    @Override
    public void stopServer() {}

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }
}