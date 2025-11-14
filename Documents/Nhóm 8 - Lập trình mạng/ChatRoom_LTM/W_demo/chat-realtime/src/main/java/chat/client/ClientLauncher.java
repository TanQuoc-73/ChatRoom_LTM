package chat.client;

import chat.core.Message;
import chat.core.MessageListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * ClientLauncher: Điểm khởi chạy của ứng dụng, xử lý input/output console.
 * - Đã loại bỏ: Logic gửi lệnh /login và chờ xác thực.
 * - CHÚ Ý: Người dùng sẽ được coi là "đăng nhập" ngay sau khi kết nối.
 */

public class ClientLauncher {
    
    // Loại bỏ cờ isLoggedIn. Client được coi là đã "đăng nhập" ngay lập tức.
    private static String currentRoom = "lobby"; // Biến quản lý phòng hiện tại
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    
    // Cờ này sẽ được dùng để kiểm tra Client có còn kết nối không
    private static volatile boolean isConnected = false; 

    public static void main(String[] args) {
        
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            
            ChatClient client = new ChatClient();
            
            client.addListener(new MessageListener() {
                @Override
                public void onMessage(Message msg) {
                    if (msg.getSender().equalsIgnoreCase("system")) {
                        // Xử lý phản hồi THAM GIA PHÒNG
                        if (msg.getContent().startsWith("/join success")) {
                            String newRoom = msg.getContent().substring(14).trim();
                            currentRoom = newRoom;
                            System.out.println(">>> Đã tham gia phòng: " + currentRoom);
                        }
                        // Xử lý các thông báo hệ thống khác
                        else {
                            System.out.println("--- [SYSTEM]: " + msg.getContent() + " ---");
                        }
                        
                    } else {
                        // In tin nhắn chat thông thường hoặc tin nhắn riêng tư
                        String prefix = msg.getRoomId().equals("PRIVATE") ? "[PRIVATE]" : "[" + msg.getRoomId() + "]"; 
                        System.out.printf("%s [%s] %s: %s%n",
                                prefix,
                                msg.getSender(), 
                                msg.getTimestamp(), 
                                msg.getContent());
                    }
                }
                
                @Override
                public void onDisconnect() {
                    // Cập nhật trạng thái ngắt kết nối
                    isConnected = false; 
                    System.err.println("\n*** KẾT NỐI ĐÃ BỊ NGẮT. Đang thoát...");
                    // Việc thoát sẽ do khối finally xử lý
                }
            });

            // 1. Nhập tên hiển thị
            System.out.print("Tên của bạn: ");
            String name = console.readLine();

            // 2. Mở kết nối
            client.connect(DEFAULT_HOST, DEFAULT_PORT);
            isConnected = true; // Đánh dấu là đã kết nối thành công
            
            System.out.println(">>> Đã kết nối thành công tới " + DEFAULT_HOST + ":" + DEFAULT_PORT + ".");
            System.out.println(">>> Bắt đầu chat. Phòng hiện tại: " + currentRoom);
            
            // THÊM: Gửi một tin nhắn thông báo cho Server về tên người dùng và phòng mặc định (dùng lệnh /join)
            // Server cần nhận được tên người dùng ban đầu. Ta dùng lệnh /join mặc dù không cần xử lý xác thực
            client.send(new Message("system", name, "/join " + currentRoom));
            
            // 3. Vòng lặp chat: gõ → gửi → chờ in
            String line;
            while ((line = console.readLine()) != null && isConnected) {
                if (line.isEmpty()) continue;

                // Xử lý các lệnh (Command Parsing)
                if (line.startsWith("/")) {
                    if (line.equalsIgnoreCase("/quit")) {
                        break; // Thoát vòng lặp
                    } else if (line.startsWith("/join ")) {
                        handleJoinCommand(client, name, line);
                    } else if (line.startsWith("/whisper ")) {
                        handleWhisperCommand(client, name, line);
                    } else if (line.equalsIgnoreCase("/room")) {
                        System.out.println(">>> Bạn đang ở phòng: " + currentRoom);
                    } else {
                        System.err.println("!!! Lệnh không hợp lệ. Các lệnh hợp lệ: /quit, /join <room>, /whisper <user> <msg>");
                    }
                } else {
                    // Gửi tin nhắn chat thông thường tới phòng hiện tại
                    client.send(new Message(currentRoom, name, line));
                }
            }
            
            // 4. Thoát
            client.disconnect();
            
        } catch (IOException e) {
            System.err.println("Lỗi I/O (Mạng/Console): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng: " + e.getMessage());
        } finally {
            System.exit(0); // Đảm bảo ứng dụng thoát
        }
    }
    
    // Hàm xử lý lệnh /join
    private static void handleJoinCommand(ChatClient client, String sender, String line) throws IOException {
        String[] parts = line.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            System.err.println("!!! Cú pháp: /join <room_name>");
            return;
        }
        String newRoom = parts[1].trim();
        // Gửi lệnh /join tới Server
        client.send(new Message("system", sender, "/join " + newRoom));
        System.out.println(">>> Đang cố gắng tham gia phòng: " + newRoom + "...");
    }

    // Hàm xử lý lệnh /whisper
    private static void handleWhisperCommand(ChatClient client, String sender, String line) throws IOException {
        String content = line.substring("/whisper ".length()).trim();
        String[] parts = content.split(" ", 2);
        
        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            System.err.println("!!! Cú pháp: /whisper <username> <message>");
            return;
        }

        String recipient = parts[0].trim();
        String messageContent = parts[1].trim();

        // Gửi tin nhắn riêng tư. roomId là PRIVATE, Content chứa người nhận + nội dung
        client.send(new Message("PRIVATE", sender, recipient + " " + messageContent));
        System.out.println(">>> Đã gửi tin nhắn riêng tới " + recipient + ".");
    }
}