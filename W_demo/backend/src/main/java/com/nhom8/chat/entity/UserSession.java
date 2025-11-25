package com.nhom8.chat.entity;

import java.time.Instant;

import com.nhom8.chat.entity.enums.DeviceType;

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
@Table(name = "user_session",
       indexes = {
           @Index(name="IX_SESSION_TOKEN", columnList="session_token"),
           @Index(name="IX_SESSION_ONLINE", columnList="is_online,last_heartbeat"),
           @Index(name="IX_SESSION_USER", columnList="user_id,connected_at")
       })
public class UserSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Column(name="session_token", length=128, nullable=false, unique=true)
    private String sessionToken;

    @Column(name="device_id", length=255) private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name="device_type", length=50)
    private DeviceType deviceType;

    @Column(name="client_info", length=255) private String clientInfo;
    @Column(name="ip_address", length=45) private String ipAddress;

    @Column(name="connected_at", nullable=false) private Instant connectedAt = Instant.now();
    @Column(name="last_heartbeat", nullable=false) private Instant lastHeartbeat = Instant.now();

    @Column(name="is_online", nullable=false) private boolean online;
    @Column(name="push_token", length=255) private String pushToken;
  
}
