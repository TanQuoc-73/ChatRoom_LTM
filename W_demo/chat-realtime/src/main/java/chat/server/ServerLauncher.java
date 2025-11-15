package chat.server;

import java.util.Scanner;

import chat.core.ChatService;
import chat.core.Message;
import chat.core.MessageListener;
import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;

public class ServerLauncher {
    public static void main(String[] args) {

        // Tạo Core ChatService với các implement SPI
        AuthGateway auth = new InMemoryAuthGateway();
        MessageStore messageStore = new InMemoryMessageStore();
        RoomStore roomStore = new InMemoryRoomStore();
        
        ChatService chatService = new ChatService(auth, messageStore, roomStore);
        
        // Tạo ChatServer với Core Service
        ChatServer server = new ChatServer(chatService);
        int port = 8080;

        // In trạng thái khởi động server
        System.out.println("đang chạy chatserver");
        System.out.println("Core Components:");
        System.out.println("   • AuthGateway: " + auth.getClass().getSimpleName());
        System.out.println("   • MessageStore: " + messageStore.getClass().getSimpleName());
        System.out.println("   • RoomStore: " + roomStore.getClass().getSimpleName());
        System.out.println("   • ChatService: " + chatService.getClass().getSimpleName());

        // Lắng nghe sự kiện từ Core cho user SYSTEM
        chatService.getSessions().registerUser("SYSTEM", new MessageListener() {
            @Override
            public void onMessage(Message message) {
                System.out.println("[" + message.getRoomId() + "] " +
                    message.getSender() + ": " + message.getContent());
            }

            @Override
            public void onEvent(Object event) {
                if (event instanceof chat.core.protocol.Events.UserJoined e) {
                    System.out.println("USER JOINED: " + e.username + " → " + e.roomId);
                } else if (event instanceof chat.core.protocol.Events.UserLeft e) {
                    System.out.println("USER LEFT: " + e.username + " ← " + e.roomId);
                } else if (event instanceof chat.core.protocol.Events.UserTyping e) {
                    String status = e.typing ? "TYPING" : "STOPPED TYPING";
                    System.out.println(status + ": " + e.username + " in " + e.roomId);
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("CORE ERROR: " + error);
            }

            @Override
            public void onDisconnect() {
                System.out.println("SYSTEM listener disconnected");
            }
        });

        // Khởi động server
        server.start(port);
        System.out.println("\nChat Server Status:");
        System.out.println("   • Port: " + port);
        System.out.println("   • Status: RUNNING");
        System.out.println("   • Protocol: JSON over TCP");
        System.out.println("   • Ready for client connections");

        // Lệnh dành cho admin
        System.out.println("\nAdmin Commands:");
        System.out.println("   • stats      - Hiển thị thống kê server");
        System.out.println("   • broadcast <msg> - Gửi thông báo admin");
        System.out.println("   • exit       - Tắt server");
        System.out.println("   • help       - Hiển thị hướng dẫn");

        // Console admin
        Scanner scanner = new Scanner(System.in);
        while (server.isRunning()) {
            try {
                System.out.print("\nserver> ");
                String cmd = scanner.nextLine().trim();
                
                switch (cmd.toLowerCase()) {
                    case "exit", "quit", "stop" -> {
                        System.out.println("Shutting down server...");
                        server.stop();
                        System.out.println("Server stopped successfully");
                    }
                    
                    case "stats", "status" -> {
                        System.out.println("Server Statistics:");
                        System.out.println("   • Connected clients: " + server.getClientCount());
                        System.out.println("   • Server status: " + (server.isRunning() ? "RUNNING" : "STOPPED"));
                        System.out.println("   • Core sessions: " + chatService.getSessions().getUsersInRoom("lobby").size() + " in lobby");
                        
                        // Hiển thị danh sách phòng đang hoạt động
                        try {
                            java.util.List<String> rooms = roomStore.getAllRooms();
                            System.out.println("   • Active rooms: " + rooms.size());
                            for (String room : rooms) {
                                java.util.Set<String> users = roomStore.getRoomUsers(room);
                                System.out.println("     - " + room + ": " + users.size() + " users");
                            }
                        } catch (Exception e) {
                            System.out.println("   • Rooms: Unable to fetch");
                        }
                    }
                    
                    case "help", "?" -> {
                        System.out.println("Available Commands:");
                        System.out.println("   • stats      - Show server statistics");
                        System.out.println("   • broadcast <msg> - Send admin message");
                        System.out.println("   • exit       - Shutdown server");
                        System.out.println("   • help       - Show this help");
                    }
                    
                    default -> {
                        if (cmd.startsWith("broadcast ")) {
                            String message = cmd.substring(10).trim();
                            if (!message.isEmpty()) {
                                server.broadcastAdminMessage("ADMIN: " + message);
                                System.out.println("Admin message sent: " + message);
                            } else {
                                System.out.println("Usage: broadcast <message>");
                            }
                        } else if (!cmd.isEmpty()) {
                            System.out.println("Unknown command: " + cmd);
                            System.out.println("Type 'help' for available commands");
                        }
                    }
                }
                
                if (cmd.equalsIgnoreCase("exit")) {
                    break;
                }
                
            } catch (Exception e) {
                System.err.println("Console error: " + e.getMessage());
            }
        }
        
        scanner.close();
        System.out.println("Server launcher terminated");
    }
}
