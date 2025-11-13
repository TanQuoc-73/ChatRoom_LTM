package chat.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    private final String roomId;
    private final String roomName;
    private final String createdBy;
    private final Date createdAt;
    private final Set<ClientHandler> members;
    private final Set<String> bannedUsers;

    private int maxMembers = 50;
    private boolean isPrivate = false;
    private String password = null;

    public ChatRoom(String roomId, String roomName, String createdBy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.createdBy = createdBy;
        this.createdAt = new Date(System.currentTimeMillis());
        this.members = ConcurrentHashMap.newKeySet();
        this.bannedUsers = ConcurrentHashMap.newKeySet();
    }

    public ChatRoom(String roomId, String roomName, String createdBy, int maxMembers, boolean isPrivate) {
        this(roomId, roomName, createdBy);
        this.maxMembers = maxMembers;
        this.isPrivate = isPrivate;
    }

    public ChatRoom(String roomId, String roomName, String createdBy, int maxMembers, boolean isPrivate, String password) {
        this(roomId, roomName, createdBy, maxMembers, isPrivate);
        this.password = password;
    }

    public synchronized boolean addMember(ClientHandler client) {
        if (members.size() >= maxMembers) {
            client.sendMessage("ERROR: Phong da day " + maxMembers + " nguoi");
            return false;
        }

        if (bannedUsers.contains(client.getUsername())) {
            client.sendMessage("ERROR: Ban da bi cam khoi phong nay");
            return false;
        }

        members.add(client);
        broadcastSystemMessage(client.getUsername() + " da tham gia phong", client);
        return true;
    }

    public synchronized void removeMember(ClientHandler client) {
        if (members.remove(client)) {
            broadcastSystemMessage(client.getUsername() + " da roi khoi phong", null);
        }
    }

    public synchronized boolean banUser(String username, String reason) {
        ClientHandler member = getMember(username);
        if (member != null) {
            removeMember(member);
            member.sendMessage("Ban da bi cam khoi phong '" + roomName + "'. Ly do: " + reason);
        }
        bannedUsers.add(username);
        broadcastSystemMessage("Thanh vien '" + username + "' da bi cam. Ly do: " + reason, null);
        return true;
    }

    public synchronized boolean unbanUser(String username) {
        boolean removed = bannedUsers.remove(username);
        if (removed) {
            broadcastSystemMessage("Thanh vien '" + username + "' da duoc go cam", null);
        }
        return removed;
    }

    public void broadcastSystemMessage(String message, ClientHandler exclude) {
        for (ClientHandler member : members) {
            if (exclude == null || !member.equals(exclude)) {
                member.sendMessage("[THONG BAO] " + message);
            }
        }
    }

    public boolean hasMember(String username) {
        return getMember(username) != null;
    }

    private ClientHandler getMember(String username) {
        for (ClientHandler member : members) {
            if (member.getUsername().equals(username)) {
                return member;
            }
        }
        return null;
    }

    public Set<String> getMemberUsernames() {
        Set<String> usernames = new HashSet<>();
        for (ClientHandler member : members) {
            usernames.add(member.getUsername());
        }
        return usernames;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getCreatedBy() { return createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public int getMemberCount() { return members.size(); }
    public int getMaxMembers() { return maxMembers; }
    public boolean isPrivate() { return isPrivate; }
    public Set<String> getBannedUsers() { return new HashSet<>(bannedUsers); }
}