package chat.server;

import chat.core.MessageListener;
import chat.core.Message;

import java.util.Scanner;

public class ServerLauncher {
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        int port = 8080;

        server.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                System.out.println("[SERVER LOG] " + message);
            }

            @Override
            public void onError(String error) {
                System.err.println("[ERROR] " + error);
            }
        });

        // Khởi động server
        server.startServer(port);
        System.out.println(" Chat server đang chạy tại cổng " + port);
        System.out.println("Gõ exit để dừng server.");

        // Cho phép admin dừng server bằng console
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String cmd = scanner.nextLine();
            if ("exit".equalsIgnoreCase(cmd)) {
                server.stopServer();
                System.out.println("Server đã dừng.");
                break;
            }
        }
        scanner.close();
    }
}
