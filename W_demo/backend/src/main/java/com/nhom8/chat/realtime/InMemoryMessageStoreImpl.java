package com.nhom8.chat.realtime;

import chat.core.Message;
import chat.core.spi.MessageStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class InMemoryMessageStoreImpl implements MessageStore {

    private static class StoredMessage {
        final Message message;
        final long timestamp;

        StoredMessage(Message message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    // roomId -> list message
    private final ConcurrentMap<String, List<StoredMessage>> store = new ConcurrentHashMap<>();

    @Override
    public void saveMessage(Message message) {
        String roomId = message.getRoomId();         // nếu khác tên getter thì sửa lại
        long ts = System.currentTimeMillis();        // hoặc dùng message.getTimestamp() nếu có

        store.computeIfAbsent(roomId,
                id -> Collections.synchronizedList(new ArrayList<>()))
             .add(new StoredMessage(message, ts));
    }

    @Override
    public List<Message> getMessages(String roomId, int limit, long beforeTimestamp) {
        List<StoredMessage> list = store.getOrDefault(roomId, Collections.emptyList());

        return list.stream()
                .filter(sm -> sm.timestamp < beforeTimestamp)
                .sorted(Comparator.comparingLong((StoredMessage sm) -> sm.timestamp).reversed())
                .limit(limit)
                .map(sm -> sm.message)
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> getMessagesSince(String roomId, long sinceTimestamp) {
        List<StoredMessage> list = store.getOrDefault(roomId, Collections.emptyList());

        return list.stream()
                .filter(sm -> sm.timestamp >= sinceTimestamp)
                .sorted(Comparator.comparingLong(sm -> sm.timestamp))
                .map(sm -> sm.message)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMessage(String messageId) {
        store.values().forEach(list ->
                list.removeIf(sm -> messageId.equals(sm.message.getId()))  // sửa getId() nếu tên khác
        );
    }
}
