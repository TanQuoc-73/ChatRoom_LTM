package chat.server;

import chat.core.ChatService;
import chat.core.MessageListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer implements ChatService {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Set<ClientHandler> clients;
    private List<MessageListener> listeners;
    private boolean isRunning;

    public ChatServer() {
        this.clients = ConcurrentHashMap.newKeySet();
        this.listeners = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    @Override
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

    @Override
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

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        notifyMessage(message);
    }

    public void notifyUserJoined(String username) {
        for (MessageListener listener : listeners) {
            listener.onUserJoined(username);
        }
    }

    public void notifyUserLeft(String username) {
        for (MessageListener listener : listeners) {
            listener.onUserLeft(username);
        }
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
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }
    @Override
    public void connectToServer(String host, int port, String username) {}
    @Override
    public void sendMessage(String message) {}
    @Override
    public void disconnect() {}
}