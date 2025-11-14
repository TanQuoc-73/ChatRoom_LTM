package chat.client;

import chat.core.Message;
import chat.core.MessageListener;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ChatClient đại diện cho MỘT người dùng.
 * - Kết nối TCP tới server
 * - Gửi & nhận tin nhắn (Message)
 * - Kích hoạt sự kiện cho các giao diện (console, GUI, v.v.)
 * - Ngắt kết nối an toàn
 */


public class ChatClient {
    
    private Socket socket;               // Ống nối TCP tới server
    private ObjectOutputStream out;      // Luồng GỬI object
    private ObjectInputStream  in;       // Luồng NHẬN object


    /* ===== Danh sách người muốn nghe sự kiện tin nhắn ===== */
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

   
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);           
        out = new ObjectOutputStream(socket.getOutputStream()); 
        in  = new ObjectInputStream(socket.getInputStream());   
        new Thread(this::listenForMessage).start(); 
    }

    
    public void send(Message msg) throws IOException {
        out.writeObject(msg); 
        out.flush();          
    }

    /* ===== NGẮT kết nối ===== */
    public void disconnect() throws IOException {
        // 1. Báo server biết mình thoát (tuân thủ giao thức)
        out.writeObject(new Message("system", "system", "/quit"));
        out.flush();
        // 2. Xóa hết listener
        listeners.clear();
        // 3. Đóng socket → giải phóng cổng + tài nguyên
        if (socket != null) socket.close();
    }


    
    /* ===== THÊM/XÓA người nghe sự kiện ===== */
    public void addListener(MessageListener l) { listeners.add(l); }
    public void removeListener(MessageListener l) { listeners.remove(l); }

    /* ===== LUỒNG RIÊNG: lắng nghe tin từ server ===== */
    private void listenForMessage() {
        try {
            while (true) { 
                Message msg = (Message) in.readObject(); // Đọc object (chặn - blocking)
                // Kích hoạt TẤT CẢ listener đã đăng ký
                for (MessageListener l : listeners) l.onMessage(msg);
            }
        } catch (EOFException e) {
            System.err.println("Server đã ngắt kết nối.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Lỗi khi nhận tin: " + e.getMessage());
        } finally {
            listeners.forEach(MessageListener::onDisconnect);
        }
    }
}