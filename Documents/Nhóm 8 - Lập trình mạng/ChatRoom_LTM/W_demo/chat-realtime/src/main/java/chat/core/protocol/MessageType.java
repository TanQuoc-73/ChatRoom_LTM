package chat.core.protocol;

public enum MessageType {
    CHAT_MESSAGE,
    PRIVATE_MESSAGE,
    JOIN_ROOM,
    LEAVE_ROOM,
    USER_JOINED,
    USER_LEFT,
    TYPING,
    STOP_TYPING,
    ERROR,
    ACK
}
