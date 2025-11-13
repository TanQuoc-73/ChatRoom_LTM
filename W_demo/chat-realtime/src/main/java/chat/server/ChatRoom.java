package chat.server;

import java.sql.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


public class ChatRoom {
    private final String roomId;
    private final String roomName;
    private final String createdBy;
    private final Date createdAt;
    private final Set<ClientHandler> members;
    // private final List<Message> messageHistory;


    private int maxMembers = 50;
    private int maxMessageHistory = 1000;
    private boolean isPrivate = false;
    private String password = null;

    public ChatRoom(String roomId,String roomName, String createdBy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.createdBy = createdBy;
        this.createdAt = new Date(System.currentTimeMillis());
        this.members = ConcurrentHashMap.newKeySet();
        // this.messageHistory = new ArrayList<>();
        // this.bannedUsers = ConcurrentHashMap.newKeySet();   
    }
    
    public ChatRoom(String roomId, String roomName,String createdBy, int maxMembers,boolean isPrivate){
        this(roomId, roomName, createdBy);
        this.maxMembers = maxMembers;
        this.isPrivate = isPrivate;
    }


    public synchronized boolean addMember(ClientHandler client) {
        if(members.size()>=maxMembers) {
            client.sendMessage("Phong da day " +maxMembers+ "nguoi");
            return false;
        }
        

        // if(bannedUsers.contains(client.getUsername())) {
        //     client.sendMessage("Ban da bi cam khoi phong nay");
        //     return false;
        // }

        if(isPrivate && password != null) {
            client.sendMessage("Phong nay la phong rieng, vui long nhap mat khau de tham gia");
            return false;
        }

        members.add(client);
        client.setCurrentRoom(this);

        broadcastSystemMessage(client.getUsername() + " da tham gia phong " + roomName);
        
        sendRecentMessages(client);
        System.out.println("Tai khoan"+client.getUsername()+" da tham gia phong "+ roomName);
        return true;
        
    }

    public synchronized void removeMember(ClientHandler client) {
        if(members.remove(client)) {
            broadcastSystemMessage(client.getUsername() + " da roi khoi phong " + roomName);
            client.setCurrentRoom(null);
            System.out.println("Tai khoan "+client.getUsername()+" da roi khoi phong "+ roomName);
        }
    }

    public void broadcast(String message, ClientHandler sender){
        if(sender != null && !members.contains(sender)){
            sender.sendMessage("ERROR:Ban khong con la thanh vien cua phong nay");
            return;
        }

        

        
    }




   
}
