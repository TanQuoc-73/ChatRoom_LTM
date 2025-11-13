package chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    private String roomName;
    private Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    public ChatRoom(String roomName) {
        this.roomName = roomName;
    }

    public void addMember(ClientHandler client) {
        members.add(client);
    }

    public void removeMember(ClientHandler client) {
        members.remove(client);
    }

    public void broadcast(String message) {
        for (ClientHandler c : members) {
            c.sendMessage(message);
        }
    }

    public String getRoomName() {
        return roomName;
    }
}
