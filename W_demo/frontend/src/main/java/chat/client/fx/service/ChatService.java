package chat.client.fx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.java_websocket.handshake.ServerHandshake; 
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import chat.client.fx.SessionStore;


public class ChatService {
    private static final String API_BASE = "http://192.168.0.100:8081/api";
    private static ChatService instance;
    
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;
    
    // WebSocket (dùng Java-WebSocket)
    private StompClient stompClient;
    private volatile boolean wsConnected = false;
    private final Object wsLock = new Object();
    
    // Subscriptions
    private final Map<Long, Consumer<JsonNode>> conversationHandlers = new ConcurrentHashMap<>();
    
    private ChatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }
    
    // ====== WEBSOCKET WITH Java-WebSocket ======
    
    public void connectWebSocket() {
        scheduler.submit(() -> {
            try {
                ensureWsConnected();
            } catch (Exception e) {
                System.err.println("WebSocket connection error: " + e.getMessage());
                scheduleReconnect();
            }
        });
    }
    
    private void ensureWsConnected() {
        synchronized (wsLock) {
            if (stompClient != null && wsConnected) return;
            
            try {
                String token = getToken();
                if (token == null || token.isBlank()) {
                    System.err.println("No token available");
                    return;
                }
                            String wsBase = API_BASE.replace("http://", "ws://");
            String endpoint = wsBase + "/ws-native" ;
            System.out.println("Connecting to WebSocket: " + endpoint);

                
                URI uri = new URI(endpoint);
                
                // Create headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Origin", "http://192.168.0.100");
                headers.put("User-Agent", "JavaFX-Client");
                headers.put("Authorization", "Bearer " + token);
                
                // Create STOMP client
                stompClient = new StompClient(uri, headers) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        super.onOpen(handshake);
                        wsConnected = true;
                        
                        // Send STOMP CONNECT frame
                        String connectFrame = "CONNECT\n" +
                                            "accept-version:1.2\n" +
                                            "heart-beat:10000,10000\n" +
                                            "\n" +
                                            "\0";
                        sendStompFrame(connectFrame);
                        System.out.println("✓ STOMP CONNECT sent");
                    }
                    
                    @Override
                    public void onMessage(String message) {
                        super.onMessage(message);
                        processStompMessage(message);
                    }
                    
                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        super.onClose(code, reason, remote);
                        wsConnected = false;
                        scheduleReconnect();
                    }
                    
                    @Override
                    public void onError(Exception ex) {
                        super.onError(ex);
                        wsConnected = false;
                        scheduleReconnect();
                    }
                };
                
                // Connect với timeout
                stompClient.connect();
                
                // Wait for connection (5 seconds max)
                long start = System.currentTimeMillis();
                while (!wsConnected && (System.currentTimeMillis() - start) < 5000) {
                    Thread.sleep(100);
                }
                
                if (!wsConnected) {
                    throw new RuntimeException("Connection timeout");
                }
                
                System.out.println("✓ WebSocket connected successfully");
                
            } catch (Exception e) {
                System.err.println("WebSocket connection failed: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("WebSocket connection failed", e);
            }
        }
    }
    
    private void processStompMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        
        try {
            // Process STOMP frame
            String[] lines = message.split("\n");
            if (lines.length < 1) return;
            
            String command = lines[0].trim();
            
            switch (command) {
                case "CONNECTED":
                    System.out.println("✓ STOMP connection established");
                    // Resubscribe
                    resubscribeAll();
                    break;
                    
                case "MESSAGE":
                    // Parse message
                    parseStompMessage(lines);
                    break;
                    
                case "ERROR":
                    System.err.println("STOMP ERROR: " + message);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing STOMP message: " + e.getMessage());
        }
    }
    
    private void parseStompMessage(String[] lines) {
        try {
            String destination = null;
            StringBuilder body = new StringBuilder();
            boolean inBody = false;
            
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                
                if (line.isEmpty()) {
                    inBody = true;
                    continue;
                }
                
                if (!inBody) {
                    if (line.startsWith("destination:")) {
                        destination = line.substring("destination:".length()).trim();
                    }
                } else {
                    body.append(line);
                    if (i < lines.length - 1) body.append("\n");
                }
            }
            
            if (destination != null && destination.startsWith("/topic/conversations/")) {
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
                    long convId = Long.parseLong(parts[3]);
                    Consumer<JsonNode> handler = conversationHandlers.get(convId);
                    if (handler != null && body.length() > 0) {
                        JsonNode message = mapper.readTree(body.toString());
                        handler.accept(message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse STOMP message: " + e.getMessage());
        }
    }
    
    private void sendStompFrame(String frame) {
        if (stompClient == null || !wsConnected) {
            System.err.println("Cannot send STOMP frame: WebSocket not connected");
            return;
        }
        
        try {
            stompClient.sendStompFrame(frame);
        } catch (Exception e) {
            System.err.println("Failed to send STOMP frame: " + e.getMessage());
        }
    }
    
    private void sendSubscribeFrame(long conversationId, String subscriptionId) {
        String destination = "/topic/conversations/" + conversationId;
        String frame = "SUBSCRIBE\n" +
                      "id:" + subscriptionId + "\n" +
                      "destination:" + destination + "\n" +
                      "\n" +
                      "\0";
        
        sendStompFrame(frame);
        System.out.println("Subscribed to: " + destination);
    }
    
    private void resubscribeAll() {
        for (Map.Entry<Long, Consumer<JsonNode>> entry : conversationHandlers.entrySet()) {
            long convId = entry.getKey();
            String subId = "sub-" + convId + "-" + System.currentTimeMillis();
            sendSubscribeFrame(convId, subId);
        }
    }
    
    public String subscribeConversation(long conversationId, Consumer<JsonNode> handler) {
        String subscriptionId = "sub-" + conversationId + "-" + System.currentTimeMillis();
        
        conversationHandlers.put(conversationId, handler);
        
        if (wsConnected) {
            sendSubscribeFrame(conversationId, subscriptionId);
        }
        
        return subscriptionId;
    }
    
    public void unsubscribe(String subscriptionId) {
        // Implementation
    }
    
    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            System.out.println("Attempting WebSocket reconnection...");
            disconnectWebSocket();
            try {
                Thread.sleep(2000);
                ensureWsConnected();
            } catch (Exception e) {
                System.err.println("Reconnection failed: " + e.getMessage());
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    public void disconnectWebSocket() {
        synchronized (wsLock) {
            if (stompClient != null) {
                try {
                    // Send DISCONNECT frame
                    String frame = "DISCONNECT\n\n\0";
                    stompClient.sendStompFrame(frame);
                    
                    // Close connection
                    stompClient.close();
                } catch (Exception e) {
                    // Ignore
                }
                stompClient = null;
            }
            wsConnected = false;
            conversationHandlers.clear();
        }
    }
    
    private String getToken() {
    return SessionStore.getSessionToken();
}

public long getCurrentUserIdSafe() {
    return SessionStore.getUserId();
}

    // ====== REST API METHODS ======
    
private HttpRequest.Builder baseRequest(String path) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + path))
            .timeout(Duration.ofSeconds(15));

    String token = getToken();
    if (token != null && !token.isBlank()) {
        builder.header("Authorization", "Bearer " + token);
    }

    long userId = getCurrentUserIdSafe();
    if (userId > 0) {
        builder.header("X-USER-ID", String.valueOf(userId));
    }

    return builder;
}

    
    public JsonNode get(String path) throws Exception {
        HttpRequest request = baseRequest(path).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body();
            if (body == null || body.isBlank()) return null;
            return mapper.readTree(body);
        }
        throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
    }
    
    public JsonNode post(String path, String body) throws Exception {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String respBody = response.body();
            if (respBody == null || respBody.isBlank()) return null;
            return mapper.readTree(respBody);
        }
        throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
    }
    
    public JsonNode post(String path, JsonNode body) throws Exception {
        return post(path, body != null ? mapper.writeValueAsString(body) : null);
    }
    
    public JsonNode put(String path, String body) throws Exception {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String respBody = response.body();
            if (respBody == null || respBody.isBlank()) return null;
            return mapper.readTree(respBody);
        }
        throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
    }
    
    public JsonNode delete(String path) throws Exception {
        HttpRequest request = baseRequest(path)
                .DELETE()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body();
            if (body == null || body.isBlank()) return null;
            return mapper.readTree(body);
        }
        throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
    }
    
    public JsonNode uploadFile(String path, Path filePath) throws Exception {
        String boundary = "Boundary-" + System.currentTimeMillis();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos), true);
        
        // File part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(filePath.getFileName().toString()).append("\"\r\n");
        
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        writer.append("Content-Type: ").append(contentType).append("\r\n\r\n");
        writer.flush();
        
        baos.write(Files.readAllBytes(filePath));
        writer.append("\r\n");
        writer.flush();
        
        // MediaType part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"mediaType\"\r\n\r\n");
        writer.append("PHOTO").append("\r\n");
        writer.flush();
        
        // End
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();
        writer.close();
        
        byte[] bytes = baos.toByteArray();
        
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body();
            if (body == null || body.isBlank()) return null;
            return mapper.readTree(body);
        }
        throw new RuntimeException("Upload Error " + response.statusCode() + ": " + response.body());
    }
    
    // Overload cho File
    public JsonNode uploadFile(String path, java.io.File file) throws Exception {
        return uploadFile(path, file.toPath());
    }
    
    public void shutdown() {
        disconnectWebSocket();
        scheduler.shutdown();
    }
}