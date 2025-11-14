package chat.client;

import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.protocol.Events;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ChatClient - Gửi/nhận Envelope, xử lý tin nhắn và sự kiện
 */
public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        new Thread(this::listen).start();
    }

    public void send(Envelope env) throws IOException {
        out.writeObject(env);
        out.flush();
    }

    public void addListener(MessageListener l) { listeners.add(l); }

    public void disconnect() throws IOException {
        if (socket != null) socket.close();
    }

    private void listen() {
        try {
            while (true) {
                Envelope env = (Envelope) in.readObject();
                handle(env);
            }
        } catch (Exception e) {
            listeners.forEach(MessageListener::onDisconnect);
        }
    }

    private void handle(Envelope env) {
        for (MessageListener l : listeners) {
            switch (env.getType()) {
                case CHAT_MESSAGE -> {
                    Message m = new Message(env.getRoomId(), env.getSender(), env.getPayload());
                    l.onMessage(m);
                }
                case PRIVATE_MESSAGE -> {
                    Message m = new Message("DM", env.getSender(), env.getPayload());
                    l.onMessage(m);
                }
                case USER_JOINED -> l.onEvent(new Events.UserJoined(env.getRoomId(), env.getSender()));
                case USER_LEFT -> l.onEvent(new Events.UserLeft(env.getRoomId(), env.getSender()));
                case ERROR -> l.onError(env.getPayload());
            }
        }
    }
}