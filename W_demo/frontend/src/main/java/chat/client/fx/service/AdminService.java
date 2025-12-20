package chat.client.fx.service;

import chat.client.fx.model.MediaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class AdminService {

    private static final AdminService INSTANCE = new AdminService();
    private final ChatService chatService = ChatService.getInstance();

    public static AdminService getInstance() {
        return INSTANCE;
    }

    // 1️⃣ Lấy danh sách bài bị ẩn
    public List<MediaDTO> getHiddenMedia() throws Exception {
        JsonNode node = chatService.get("/moderation/media/hidden");
        return chatService.getMapper()
                .convertValue(node, new TypeReference<List<MediaDTO>>() {});
    }

    // 2️⃣ Xóa bài
    public void deleteMedia(Long mediaId) throws Exception {
        chatService.delete("/moderation/media/" + mediaId);
    }

    // 3️⃣ Khoá user
    public void blockUser(Long userId) throws Exception {
        chatService.put("/admin/users/" + userId + "/block", "");
    }

    // 4️⃣ Mở khoá user
    public void unblockUser(Long userId) throws Exception {
        chatService.put("/admin/users/" + userId + "/unblock", "");
    }
}
