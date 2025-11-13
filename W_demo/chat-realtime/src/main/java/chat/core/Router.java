package chat.core;

import chat.core.protocol.Envelope;
import chat.core.protocol.Events;
import chat.core.protocol.MessageType;

public class Router {
    private final SessionRegistry sessionRegistry;

    public Router(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void route(Envelope envelope) {
        if (envelope == null || envelope.getType() == null) return;

        switch (envelope.getType()) {
            case CHAT_MESSAGE -> handleChat(envelope);
            case PRIVATE_MESSAGE -> handleDM(envelope);
            case JOIN_ROOM -> handleJoin(envelope);
            case LEAVE_ROOM -> handleLeave(envelope);
            case TYPING, STOP_TYPING -> handleTyping(envelope);
            default -> handleUnknown(envelope);
        }
    }

    private void handleChat(Envelope env) {
        sessionRegistry.broadcastToRoom(env.getRoomId(), env);
    }

    private void handleDM(Envelope env) {
        if (env.getReceiver() == null) return;
        MessageListener receiver = sessionRegistry.getUserListener(env.getReceiver());
        if (receiver != null)
            receiver.onMessage(new Message("DM", env.getSender(), env.getPayload()));
    }

    private void handleJoin(Envelope env) {
        sessionRegistry.joinRoom(env.getSender(), env.getRoomId());
        sessionRegistry.broadcastToRoomExcept(env.getRoomId(), env.getSender(),
                new Events.UserJoined(env.getRoomId(), env.getSender()));
    }

    private void handleLeave(Envelope env) {
        sessionRegistry.leaveRoom(env.getSender(), env.getRoomId());
        sessionRegistry.broadcastToRoomExcept(env.getRoomId(), env.getSender(),
                new Events.UserLeft(env.getRoomId(), env.getSender()));
    }

    private void handleTyping(Envelope env) {
        boolean typing = env.getType() == MessageType.TYPING;
        sessionRegistry.broadcastToRoomExcept(env.getRoomId(), env.getSender(),
                new Events.UserTyping(env.getRoomId(), env.getSender(), typing));
    }

    private void handleUnknown(Envelope env) {
        MessageListener sender = sessionRegistry.getUserListener(env.getSender());
        if (sender != null) sender.onError("Unknown type: " + env.getType());
    }
}
