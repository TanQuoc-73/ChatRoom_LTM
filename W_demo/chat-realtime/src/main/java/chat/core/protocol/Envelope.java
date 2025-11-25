package chat.core.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Envelope implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String roomId;
    private String sender;
    private String receiver; 
    private long timestamp;
    private String payload;
    private Map<String, Object> metadata;

    public Envelope() {
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public Envelope(MessageType type, String roomId, String sender, String payload) {
        this();
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void addMetadata(String k, Object v) { metadata.put(k, v); }

    @Override
    public String toString() {
        return "Envelope{" +
                "type=" + type +
                ", room='" + roomId + '\'' +
                ", sender='" + sender + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
