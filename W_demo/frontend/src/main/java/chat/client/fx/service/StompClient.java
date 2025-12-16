package chat.client.fx.service;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StompClient extends WebSocketClient {
    
    public StompClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
        
        // Set STOMP protocol
        List<IProtocol> protocols = new ArrayList<>();
        protocols.add(new Protocol("v10.stomp"));
        protocols.add(new Protocol("v11.stomp"));
        protocols.add(new Protocol("v12.stomp"));
        
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✓ WebSocket connected");
    }
    
    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed: " + reason);
    }
    
    @Override
    public void onError(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
    }
    
    public void sendStompFrame(String frame) {
        this.send(frame);
    }
}