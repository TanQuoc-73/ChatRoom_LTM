package chat.client;

import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.protocol.Events;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

<<<<<<< HEAD
/**
 * Console Client - Chỉ chat, join, whisper, quit
 */
public class ClientLauncher {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static volatile boolean running = true;
    private static String room = "lobby";

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Tên bạn: ");
        String inputName = console.readLine().trim();
        final String name = inputName.isEmpty() 
            ? "User" + (int)(Math.random() * 1000) 
            : inputName;

        client.addListener(new MessageListener() {
            @Override public void onMessage(Message m) {
                String prefix = "DM".equals(m.getRoomId()) ? "[DM]" : "[" + m.getRoomId() + "]";
                System.out.println(prefix + " " + m.getSender() + ": " + m.getContent());
            }

            @Override public void onEvent(Object e) {
                if (e instanceof Events.UserJoined j) {
                    System.out.println(">>> " + j.username + " vào phòng " + j.roomId);
                    if (j.username.equals(name)) room = j.roomId;
                } else if (e instanceof Events.UserLeft l) {
                    System.out.println(">>> " + l.username + " rời phòng");
                }
            }

            @Override public void onDisconnect() {
                running = false;
                System.out.println("\n*** Mất kết nối!");
            }
        });

        client.connect(HOST, PORT);
        System.out.println("Đã kết nối. Gõ tin nhắn hoặc lệnh:");
        System.out.println("  /join <phòng>  |  /whisper <tên> <tin>  |  /quit");

        // Join phòng mặc định
        client.send(new Envelope(MessageType.JOIN_ROOM, room, name, null));

        // Vòng lặp nhập
        String line;
        while (running && (line = console.readLine()) != null) {
            if (line.isEmpty()) continue;

            if (line.startsWith("/join ")) {
                String newRoom = line.substring(6).trim();
                client.send(new Envelope(MessageType.JOIN_ROOM, newRoom, name, null));
                System.out.println("Đang vào phòng: " + newRoom);
            }
            else if (line.startsWith("/whisper ")) {
                String rest = line.substring(9);
                int space = rest.indexOf(' ');
                if (space == -1) {
                    System.out.println("Cú pháp: /whisper <tên> <tin>");
                    continue;
                }
                String to = rest.substring(0, space);
                String msg = rest.substring(space + 1);
                Envelope dm = new Envelope(MessageType.PRIVATE_MESSAGE, null, name, msg);
                dm.setReceiver(to);
                client.send(dm);
                System.out.println("[DM → " + to + "]: " + msg);
            }
            else if (line.equals("/quit")) {
                break;
            }
            else {
                client.send(new Envelope(MessageType.CHAT_MESSAGE, room, name, line));
            }
        }

        client.disconnect();
=======
import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Events;

public class ClientLauncher {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static volatile boolean running = true; // trạng thái vòng lặp chính
    private static String currentRoom = "lobby";    // phòng mặc định
    private static final Map<String, Boolean> joinedRooms = new HashMap<>(); // lưu các phòng đã join

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.print("Tên bạn: ");
            String inputName = console.readLine().trim();
            final String name = inputName.isEmpty()
                ? "User" + (int)(Math.random() * 1000)
                : inputName;

            System.out.println("đang knoi server...");
            client.connect(HOST, PORT);
            System.out.println("kết nối perfect");

            // Listener xử lý message từ server
            client.addListener(new MessageListener() {
                @Override public void onMessage(Message m) {
                    String prefix = "DM".equals(m.getRoomId()) ? "[DM]" : "[" + m.getRoomId() + "]";
                    System.out.println(prefix + " " + m.getSender() + ": " + m.getContent());
                }

                @Override public void onEvent(Object e) {
                    // Sự kiện join phòng
                    if (e instanceof Events.UserJoined j) {
                        System.out.println(">>> " + j.username + " vào phòng " + j.roomId);

                        // chính mình join cập nhật room hiện tại
                        if (j.username.equals(name)) {
                            joinedRooms.put(j.roomId, true);
                            currentRoom = j.roomId; // chuyển phòng khi server xác nhận
                            System.out.println("đã tham gia và tới phòng " + j.roomId);
                        }
                    }
                    // Sự kiện rời phòng
                    else if (e instanceof Events.UserLeft l) {
                        System.out.println(">>> " + l.username + " rời phòng " + l.roomId);

                        if (l.username.equals(name)) {
                            joinedRooms.remove(l.roomId);

                            // Nếu rời đúng phòng hiện tại, đưa về phòng lobby
                            if (l.roomId.equals(currentRoom)) {
                                currentRoom = "lobby";
                                System.out.println("di chuyển về phòng mặc định lobby");
                            }
                        }
                    }
                }

                @Override public void onError(String error) {
                    System.out.println("lỗi " + error);
                }

                @Override public void onDisconnect() {
                    running = false;
                    System.out.println("Mất knoi ");
                }
            });

            // Hướng dẫn lệnh
            System.out.println("\nLenh su dung:");
            System.out.println("/join <phong> - vào phòng mới");
            System.out.println("/rooms        - xem các phòng đã tham gia");
            System.out.println("/current      - Xxem phòng hiện tại");
            System.out.println("/quit         - thoát ctrinh");
            System.out.println("/whisper      - nt riêng ");
            System.out.println("  <tin nhan>  - gửi tn trong phòng");

            // Tự động join phòng lobby
            client.joinRoom(currentRoom, name);
            joinedRooms.put(currentRoom, true); // thêm vào danh sách phòng đã join
            System.out.println("đã tự động tham gia phòng: " + currentRoom);

            // Vòng lặp nhập lệnh
            String line;
            while (running && (line = console.readLine()) != null) {
                if (line.isEmpty()) continue;

                try {
                    if (line.startsWith("/join ")) {
                        String newRoom = line.substring(6).trim();
                        if (!newRoom.isEmpty()) {
                            // Yêu cầu join phòng, đợi server xác nhận
                            client.joinRoom(newRoom, name);
                            System.out.println("xin vào phòng " + newRoom);
                        }
                    }
                    else if (line.equals("/rooms")) {
                        System.out.println("các phòng tgia");
                        for (String room : joinedRooms.keySet()) {
                            String indicator = room.equals(currentRoom) ? " (chờ tí)" : "";
                            System.out.println("  - " + room + indicator);
                        }
                    }
                    else if (line.equals("/current")) {
                        System.out.println("phòng hiện tại " + currentRoom);
                        System.out.println("joinedRooms: " + joinedRooms.keySet());
                    }
                    else if (line.startsWith("/whisper ")) {
                        String rest = line.substring(9);
                        int space = rest.indexOf(' ');
                        if (space == -1) {
                            System.out.println("Cú pháp: /whisper <tên> <tin>");
                            continue;
                        }
                        String to = rest.substring(0, space);
                        String msg = rest.substring(space + 1);
                        client.sendPrivateMessage(to, name, msg);
                        System.out.println("[BẠN → " + to + "]: " + msg);
                    }
                    else if (line.equals("/quit")) {
                        break;
                    }
                    else if (line.equals("/debug")) {
                        System.out.println("=== DEBUG ===");
                        System.out.println("currentRoom: " + currentRoom);
                        System.out.println("joinedRooms: " + joinedRooms);
                        System.out.println("running: " + running);
                    }
                    else {
                        // Gửi tin nhắn vào phòng hiện tại
                        if (!joinedRooms.isEmpty()) {
                            client.sendMessage(currentRoom, name, line);
                            System.out.println("[Ban -> " + currentRoom + "]: " + line);
                        } else {
                            System.out.println("chưa tgia phòng oi thoi chét");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("helpme(lỗi) " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("lỗi " + e.getMessage());
        } finally {
            try {
                client.disconnect();
                console.close();
            } catch (Exception e) {
                // bỏ qua lỗi đóng
            }
            System.out.println("xin vĩnh biệt cụ!!!");
        }
>>>>>>> 6b94cdfc5cf0d2a1e923f1190cec154162da70e2
    }
}
