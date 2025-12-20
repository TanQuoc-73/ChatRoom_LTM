package chat.client.fx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)  // ← THÊM DÒNG NÀY LÀ XỬ LÝ XONG!
public class Post {
    private Long id;
    private Long userId;
    private String authorUsername;
    private String authorAvatar;
    private String fileUrl;       // ← Bạn dùng cái này để hiển thị ảnh
    private String caption;
    private String uploadedAt;

    // Nếu bạn muốn dùng thêm thông tin khác từ backend sau này, có thể thêm:
    // private String fileName;
    // private String filePath;   // ← Có thể thêm nếu cần, nhưng không bắt buộc

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}