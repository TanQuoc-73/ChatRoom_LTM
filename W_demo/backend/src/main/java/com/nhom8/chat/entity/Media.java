package com.nhom8.chat.entity;

import com.nhom8.chat.entity.enums.MediaType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "media",
       indexes = {
         @Index(name="IX_MEDIA_USER", columnList="user_id,media_type,uploaded_at"),
         @Index(name="IX_MEDIA_TYPE", columnList="media_type,is_temp")
       })
public class Media {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20, nullable = false)
    private MediaType mediaType;

    private Integer width;
    private Integer height;
    private Integer duration;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "is_temp", nullable = false)
    private boolean temp = true;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();
    
}
