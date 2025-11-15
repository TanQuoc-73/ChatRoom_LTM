package chat.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import chat.core.Message;


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
    }
}