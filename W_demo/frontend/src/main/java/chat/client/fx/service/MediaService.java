package chat.client.fx.service;

import chat.client.fx.model.MediaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing media (photos, videos) via backend API
 */
public class MediaService {
    private static MediaService instance;
    private final ChatService chatService;
    private final ObjectMapper mapper;

    private MediaService() {
        this.chatService = ChatService.getInstance();
        this.mapper = new ObjectMapper();
        // Configure mapper to handle Instant and other types
        this.mapper.findAndRegisterModules();
    }

    public static synchronized MediaService getInstance() {
        if (instance == null) {
            instance = new MediaService();
        }
        return instance;
    }

    /**
     * Upload a media file
     * 
     * @param file      The file to upload
     * @param mediaType Type of media (PHOTO, VIDEO, FILE, etc.)
     * @param caption   Optional caption for photos
     * @return MediaDTO of uploaded file
     */
    public MediaDTO uploadMedia(File file, String mediaType, String caption) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File không tồn tại");
        }

        // Use ChatService's uploadFile method - it already handles multipart
        JsonNode response = chatService.uploadFile("/media/upload", file);

        if (response == null) {
            throw new RuntimeException("Upload thất bại: không có response");
        }

        // Parse response to MediaDTO
        return mapper.treeToValue(response, MediaDTO.class);
    }

    /**
     * Get all media of current user
     * 
     * @param mediaType Optional filter by type (PHOTO, VIDEO, etc.). Pass null for
     *                  all.
     * @return List of MediaDTO
     */
    public List<MediaDTO> getMyMedia(String mediaType) throws Exception {
        String path = "/media/my-media";
        if (mediaType != null && !mediaType.isEmpty()) {
            path += "?mediaType=" + mediaType;
        }

        JsonNode response = chatService.get(path);

        if (response == null) {
            return new ArrayList<>();
        }

        // Parse array response
        if (response.isArray()) {
            return mapper.convertValue(response, new TypeReference<List<MediaDTO>>() {
            });
        }

        return new ArrayList<>();
    }

    /**
     * Get a specific media by ID
     * 
     * @param mediaId Media ID
     * @return MediaDTO
     */
    public MediaDTO getMedia(Long mediaId) throws Exception {
        if (mediaId == null) {
            throw new IllegalArgumentException("Media ID không được null");
        }

        JsonNode response = chatService.get("/media/" + mediaId);

        if (response == null) {
            throw new RuntimeException("Không tìm thấy media");
        }

        return mapper.treeToValue(response, MediaDTO.class);
    }

    /**
     * Delete a media
     * 
     * @param mediaId Media ID to delete
     */
    public void deleteMedia(Long mediaId) throws Exception {
        if (mediaId == null) {
            throw new IllegalArgumentException("Media ID không được null");
        }

        chatService.delete("/media/" + mediaId);
    }

    /**
     * Get full URL for media file
     * 
     * @param media MediaDTO
     * @return Full URL to access the media
     */
    public String getMediaUrl(MediaDTO media) {
        if (media == null || media.getFileUrl() == null) {
            return null;
        }

        String fileUrl = media.getFileUrl();

        // If already a full URL, return as is
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return fileUrl;
        }

        // Otherwise construct full URL
        String baseUrl = "http://localhost:8081";

        // Ensure proper path formatting
        if (!fileUrl.startsWith("/")) {
            fileUrl = "/" + fileUrl;
        }

        return baseUrl + fileUrl;
    }
}
