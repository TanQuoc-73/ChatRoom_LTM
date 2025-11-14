package chat.core;

import chat.core.spi.AuthGateway;

public class PermissionChecker {
    private final AuthGateway authGateway;

    public PermissionChecker(AuthGateway authGateway) {
        this.authGateway = authGateway;
    }

    public boolean canSendMessage(String username, String roomId) {
        return authGateway.isUserInRoom(username, roomId);
    }

    public boolean canJoinRoom(String username, String roomId) {
        return authGateway.canUserJoinRoom(username, roomId);
    }

    public boolean canCreateRoom(String username) {
        return true;
    }
}
