package chat.client;

import chat.core.Message;
import chat.core.MessageListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ClientLauncher {
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();

        // 1. Đăng ký sự kiện nhận tin
        client.addListener(msg ->
            System.out.printf("[%d] %s: %s%n",
                msg.getTimestamp(), msg.getSender(), msg.getContent()));

        // 2. Nhập tên
        System.out.print("Tên của bạn: ");
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String name = console.readLine();

        // 3. Kết nối tới server
        client.connect("localhost", 8080);

        // 4. Chat loop
        String line;
        while (!(line = console.readLine()).equalsIgnoreCase("/quit")) {
            // dùng constructor 3 tham số: roomId = "lobby"
            client.send(new Message("lobby", name, line));
        }

        // 5. Thoát
        client.disconnect();
        System.exit(0);
    }
}