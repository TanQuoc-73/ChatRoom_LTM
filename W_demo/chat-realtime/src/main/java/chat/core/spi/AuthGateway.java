package chat.core.spi;

public interface AuthGateway {
    boolean authenticate(String username, String password);
    boolean isUserInRoom(String username, String roomId);
    boolean canUserJoinRoom(String username, String roomId);

    default boolean isAuthenticated(String username) { return true; }
}
