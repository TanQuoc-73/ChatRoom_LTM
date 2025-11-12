package chat.core.spi;

import java.util.Set;

public interface DeliveryStore {
    void markMessageDelivered(String messageId, String userId);
    void markMessageRead(String messageId, String userId);
    Set<String> getUndeliveredMessages(String userId);
    boolean isMessageDelivered(String messageId, String userId);
}
