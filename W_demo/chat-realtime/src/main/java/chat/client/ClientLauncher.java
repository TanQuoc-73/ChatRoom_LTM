package chat.client;

import java.util.Scanner;

public class ClientLauncher {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        Scanner sc = new Scanner(System.in);
        System.out.print("Nhập tên của bạn: ");
        String username = sc.nextLine();

        ChatClient client = new ChatClient();
        client.addMessageListener(new chat.core.MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                System.out.println(message);
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

        client.connectToServer(host, port, username);

        // Nhập tin nhắn từ console
        while (true) {
            String msg = sc.nextLine();
            if ("exit".equalsIgnoreCase(msg)) {
                client.disconnect();
                break;
            }
            client.sendMessage(msg);
        }

        sc.close();
    }
}
