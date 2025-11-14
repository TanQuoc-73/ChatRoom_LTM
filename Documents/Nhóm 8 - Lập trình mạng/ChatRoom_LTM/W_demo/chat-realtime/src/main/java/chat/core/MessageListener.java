package chat.core;

public interface MessageListener {
    void onMessage(Message message);
    default void onEvent(Object event) {}
    default void onError(String error) {}
    default void onDisconnect() {}
    default void onAck(String cid) {}
}
