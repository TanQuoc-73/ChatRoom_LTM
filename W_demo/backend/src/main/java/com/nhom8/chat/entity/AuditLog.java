package com.nhom8.chat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "audit_log",
       indexes = {
         @Index(name="IX_AUDIT_USER_TIME", columnList="user_id,created_at"),
         @Index(name="IX_AUDIT_ACTION_TIME", columnList="action_type,created_at")
       })
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id")
    private AppUser user; // nullable

    @Column(name="action_type", length=100, nullable=false) private String actionType;

    @Column(name="target_entity_type", length=50) private String targetEntityType;
    @Column(name="target_entity_id") private Long targetEntityId;

    @Lob @Column(name="old_values") private String oldValues;
    @Lob @Column(name="new_values") private String newValues;

    @Column(name="ip_address", length=45) private String ipAddress;
    @Column(name="user_agent", length=500) private String userAgent;

    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
    // getters/setters
}
