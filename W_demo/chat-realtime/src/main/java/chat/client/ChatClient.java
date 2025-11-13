package chat.client;

import chat.core.Message;
import chat.core.MessageListener;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        new Thread(this::listenForMessage).start();
    }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
    }

    public void disconnect() throws IOException {
        listeners.clear();
        if (socket != null) socket.close();
    }

    public void addListener(MessageListener l) { listeners.add(l); }
    public void removeListener(MessageListener l) { listeners.remove(l); }

    private void listenForMessage() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                for (MessageListener l : listeners) l.onMessage(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Mất kết nối tới server.");
        }
    }
}