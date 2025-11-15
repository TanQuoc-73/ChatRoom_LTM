package chat.core;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String roomId;
    private final String sender;
    private final String content;
    private final long timestamp;

    public Message(String roomId, String sender, String content) {
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.id = generateId();
    }

    private String generateId() {
        return roomId + "_" + sender + "_" + timestamp;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    @Override public String toString() {
        return "Message{" + "room=" + roomId + ", sender=" + sender + ", content='" + content + "'}";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}
