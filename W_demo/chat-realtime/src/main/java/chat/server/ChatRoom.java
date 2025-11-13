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


    public synchronized boolean addMember(ClientHandler client) {
        if (members.size() >= maxMembers) {
            client.sendMessage("ERROR: Phòng đã đầy " + maxMembers + " người");
            return false;
        }

        if (bannedUsers.contains(client.getUsername())) {
            client.sendMessage("ERROR: Bạn đã bị cấm khỏi phòng này");
            return false;
        }

        if (isPrivate && password != null) {
            client.sendMessage("ERROR: Phòng riêng, cần mật khẩu để tham gia");
            return false;
        }

        members.add(client);
        client.setCurrentRoom(this);

        broadcastSystemMessage(client.getUsername() + " đã tham gia phòng", client);
        
        System.out.println("ERROR:" + client.getUsername() + " đã tham gia phòng " + roomName);
        return true;
    }


    public synchronized void removeMember(ClientHandler client) {
        if (members.remove(client)) {
            client.setCurrentRoom(null);
            broadcastSystemMessage(client.getUsername() + " đã rời khỏi phòng", null);
            System.out.println("ERROR: " + client.getUsername() + " đã rời khỏi phòng " + roomName);
        }
    }


    public synchronized boolean banUser(String username, String reason) {
        ClientHandler member = getMember(username);
        if (member != null) {
            removeMember(member);
            member.sendMessage("Bạn đã bị cấm khỏi phòng '" + roomName + "'. Lý do: " + reason);
        }
        
        bannedUsers.add(username);
        broadcastSystemMessage("Thành viên '" + username + "' đã bị cấm. Lý do: " + reason, null);
        return true;
    }


    public synchronized boolean unbanUser(String username) {
        boolean removed = bannedUsers.remove(username);
        if (removed) {
            broadcastSystemMessage("Thành viên '" + username + "' đã được gỡ cấm", null);
        }
        return removed;
    }


    public void broadcastSystemMessage(String message, ClientHandler exclude) {
        for (ClientHandler member : members) {
            if (exclude == null || !member.equals(exclude)) {
                member.sendMessage("[THÔNG BÁO] " + message);
            }
        }
    }

    //ktra thanh vien trong phong
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

    @Override
    public String toString() {
        return String.format("ChatRoom{id='%s', name='%s', members=%d/%d}",
                roomId, roomName, members.size(), maxMembers);
    }
}