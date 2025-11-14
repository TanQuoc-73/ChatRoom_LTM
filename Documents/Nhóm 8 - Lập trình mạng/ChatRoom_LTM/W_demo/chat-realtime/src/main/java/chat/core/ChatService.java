package chat.core;

import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;

public class ChatService {
    private final SessionRegistry sessions;
    private final Router router;
    private final AuthGateway auth;
    private final MessageStore messages;
    private final RoomStore rooms;
    private final RateLimiter limiter;

    public ChatService(AuthGateway auth, MessageStore messages, RoomStore rooms) {
        this.auth = auth;
        this.messages = messages;
        this.rooms = rooms;
        this.sessions = new SessionRegistry();
        this.router = new Router(sessions);
        this.limiter = new RateLimiter();
    }

    public boolean login(String username, String password, MessageListener listener) {
        if (auth.authenticate(username, password)) {
            sessions.registerUser(username, listener);
            return true;
        }
        return false;
    }

    public void logout(String username) {
        sessions.unregisterUser(username);
    }

    public void joinRoom(String username, String roomId) {
        if (!limiter.isAllowed(username, "join_room")) return;
        if (auth.canUserJoinRoom(username, roomId)) {
            rooms.addUserToRoom(roomId, username);
            sessions.joinRoom(username, roomId);
            router.route(new Envelope(MessageType.JOIN_ROOM, roomId, username, "joined"));
        }
    }

    public void sendMessage(Envelope envelope) {
        if (!limiter.isAllowed(envelope.getSender(), "message")) return;
        Message msg = new Message(envelope.getRoomId(), envelope.getSender(), envelope.getPayload());
        messages.saveMessage(msg);
        router.route(envelope);
    }

    public void leaveRoom(String username, String roomId) {
        sessions.leaveRoom(username, roomId);
        rooms.removeUserFromRoom(roomId, username);
        router.route(new Envelope(MessageType.LEAVE_ROOM, roomId, username, "left"));
    }

    public SessionRegistry getSessions() { return sessions; }
    public Router getRouter() { return router; }
}
