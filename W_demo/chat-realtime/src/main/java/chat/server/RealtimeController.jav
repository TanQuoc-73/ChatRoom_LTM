package chat.server;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
public class RealtimeController {

    private final ChatServer chatServer; // bean realtime server

    @PostMapping("/message")
    public void onMessage(@RequestBody Map<String,Object> body) {
        // body keys: messageId, conversationId, senderId, content, clientCid, sentAt
        chatServer.broadcastMessageFromBackend(body);
    }

    @PostMapping("/read")
    public void onRead(@RequestBody Map<String,Object> body) {
        chatServer.broadcastReadReceipt(body);
    }
}
