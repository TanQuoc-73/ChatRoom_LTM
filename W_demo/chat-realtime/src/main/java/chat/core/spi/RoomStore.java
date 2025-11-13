package chat.core.spi;

import java.util.List;
import java.util.Set;

public interface RoomStore {
    boolean roomExists(String roomId);
    void createRoom(String roomId, String createdBy);
    void deleteRoom(String roomId);
    Set<String> getRoomUsers(String roomId);
    List<String> getAllRooms();
    void addUserToRoom(String roomId, String username);
    void removeUserFromRoom(String roomId, String username);
}
