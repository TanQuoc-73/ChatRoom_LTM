package com.nhom8.chat.entity;

import java.time.Instant;

import com.nhom8.chat.entity.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification",
       indexes = {
         @Index(name="IX_NOTIFICATION_USER", columnList="user_id,is_read,created_at"),
         @Index(name="IX_NOTIFICATION_TYPE", columnList="type,created_at")
       })
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name="type", length=50, nullable=false)
    private NotificationType type;

    @Column(length=255, nullable=false) private String title;
    @Column(length=500) private String content;

    @Column(name="related_entity_type", length=50) private String relatedEntityType;
    @Column(name="related_entity_id") private Long relatedEntityId;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sender_id")
    private AppUser sender;

    @Column(name="is_read", nullable=false) private boolean read;
    @Column(name="read_at") private Instant readAt;

    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();

}
