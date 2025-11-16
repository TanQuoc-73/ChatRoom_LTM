package chat.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
<<<<<<< HEAD
import chat.core.protocol.MessageType;
import chat.core.protocol.Events;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
=======
import chat.core.protocol.Events;
import chat.core.protocol.MessageType;
>>>>>>> 6b94cdfc5cf0d2a1e923f1190cec154162da70e2

/**
 * ChatClient - Gửi/nhận Envelope, xử lý tin nhắn và sự kiện
 */
public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean connected = false; // trạng thái kết nối
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>(); // thread-safe
    private final ObjectMapper mapper = new ObjectMapper(); // dùng để parse JSON

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
<<<<<<< HEAD
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
            // Temporary debug to understand disconnect cause
            e.printStackTrace();
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
                case JOIN_ROOM -> l.onEvent(new Events.UserJoined(env.getRoomId(), env.getSender()));
                case LEAVE_ROOM -> l.onEvent(new Events.UserLeft(env.getRoomId(), env.getSender()));
                case USER_JOINED -> l.onEvent(new Events.UserJoined(env.getRoomId(), env.getSender()));
                case USER_LEFT -> l.onEvent(new Events.UserLeft(env.getRoomId(), env.getSender()));
                case TYPING -> l.onEvent(new Events.UserTyping(env.getRoomId(), env.getSender(), true));
                case STOP_TYPING -> l.onEvent(new Events.UserTyping(env.getRoomId(), env.getSender(), false));
                case ERROR -> l.onError(env.getPayload());
                case ACK -> l.onAck(env.getPayload());
            }
=======
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected = true;

        // Thread lắng nghe dữ liệu từ server
        Thread listenerThread = new Thread(this::listenLoop, "chat-client-listener");
        listenerThread.setDaemon(true); // thread chạy nền
        listenerThread.start();
    }


    public synchronized void send(Envelope env) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }
        String json = mapper.writeValueAsString(env); // chuyển object thành JSON
        writer.write(json);
        writer.write("\n"); // server đọc theo dòng
        writer.flush();
    }
    
    public void joinRoom(String roomId, String username) throws IOException {
        send(new Envelope(MessageType.JOIN_ROOM, roomId, username, ""));
    }

    public void sendMessage(String roomId, String username, String content) throws IOException {
        send(new Envelope(MessageType.CHAT_MESSAGE, roomId, username, content));
    }

    public void leaveRoom(String roomId, String username) throws IOException {
        send(new Envelope(MessageType.LEAVE_ROOM, roomId, username, ""));
    }

    public void sendPrivateMessage(String toUser, String fromUser, String content) throws IOException {
        Envelope env = new Envelope(MessageType.PRIVATE_MESSAGE, null, fromUser, content);
        env.setReceiver(toUser); // thiết lập người nhận tin riêng
        send(env);
    }

    public void sendTypingIndicator(String roomId, String username, boolean typing) throws IOException {
        MessageType type = typing ? MessageType.TYPING : MessageType.STOP_TYPING;
        send(new Envelope(type, roomId, username, ""));
    }
    
    public void addListener(MessageListener listener) {
        listeners.add(listener); // đăng ký callback
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed(); // kiểm tra socket còn
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // đóng kết nối
            }
        } catch (IOException e) {
            // bỏ qua lỗi khi đóng
        } finally {
            listeners.forEach(MessageListener::onDisconnect); // callback disconnect
        }
    }

    // Xử lý vòng lặp lắng nghe tin nhắn từ server
    private void listenLoop() {
        try {
            String line;
            while (isConnected() && (line = reader.readLine()) != null) {
                processIncomingMessage(line);
            }
        } catch (IOException e) {
            // lỗi đọc khi disconnect là bình thường
            if (isConnected()) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in listener: " + e.getMessage());
        } finally {
            connected = false;
            listeners.forEach(MessageListener::onDisconnect);
        }
    }

    private void processIncomingMessage(String jsonLine) {
        try {
            Envelope envelope = mapper.readValue(jsonLine, Envelope.class); // parse JSON -> Envelope
            dispatchEnvelope(envelope);
        } catch (IOException e) {
            System.err.println("Failed to parse JSON: " + jsonLine);
            listeners.forEach(l -> l.onError("Protocol error: invalid JSON"));
        }
    }

    // Phân phối sự kiện dựa theo loại 
    private void dispatchEnvelope(Envelope env) {
        for (MessageListener listener : listeners) {
            try {
                handleEnvelopeForListener(env, listener);
            } catch (Exception e) {
                System.err.println("Error in message listener: " + e.getMessage());
            }
        }
    }

    private void handleEnvelopeForListener(Envelope env, MessageListener listener) {
        switch (env.getType()) {
            case CHAT_MESSAGE -> {
                Message message = new Message(env.getRoomId(), env.getSender(), env.getPayload());
                listener.onMessage(message); // callback tin nhắn thường
            }
            
            case PRIVATE_MESSAGE -> {
                Message message = new Message("DM", env.getSender(), env.getPayload());
                listener.onMessage(message); // callback tin nhắn riêng
            }
            
            case USER_JOINED ->
                listener.onEvent(new Events.UserJoined(env.getRoomId(), env.getSender())); // user vào phòng
            
            case USER_LEFT ->
                listener.onEvent(new Events.UserLeft(env.getRoomId(), env.getSender())); // user rời phòng
            
            case TYPING ->
                listener.onEvent(new Events.UserTyping(env.getRoomId(), env.getSender(), true)); // đang gõ
            
            case STOP_TYPING ->
                listener.onEvent(new Events.UserTyping(env.getRoomId(), env.getSender(), false)); // ngừng gõ
            
            case ERROR ->
                listener.onError(env.getPayload()); 
            
            case ACK ->
                listener.onAck(env.getPayload()); // server xác nhận
            
            default ->
                System.out.println("Unhandled message type: " + env.getType());
>>>>>>> 6b94cdfc5cf0d2a1e923f1190cec154162da70e2
        }
    }
}