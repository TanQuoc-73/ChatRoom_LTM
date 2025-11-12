package chat.server;

import chat.core.MessageListener;

import java.util.Scanner;

public class ServerLauncher {
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        int port = 8080;

        server.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                System.out.println("[SERVER LOG] " + message);
            }

            @Override
            public void onUserJoined(String username) {
                System.out.println("[JOIN] " + username + " đã vào phòng.");
            }

            @Override
            public void onUserLeft(String username) {
                System.out.println("[LEAVE] " + username + " đã rời khỏi phòng.");
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
