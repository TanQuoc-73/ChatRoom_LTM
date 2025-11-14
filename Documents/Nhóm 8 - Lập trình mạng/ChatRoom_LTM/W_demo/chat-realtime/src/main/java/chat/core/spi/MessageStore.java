package chat.core.spi;

import java.util.List;

import chat.core.Message;

public interface MessageStore {
    void saveMessage(Message message);
    List<Message> getMessages(String roomId, int limit, long beforeTimestamp);
    List<Message> getMessagesSince(String roomId, long sinceTimestamp);
    void deleteMessage(String messageId);
}
