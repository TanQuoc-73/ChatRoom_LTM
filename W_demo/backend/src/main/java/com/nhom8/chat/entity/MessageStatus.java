package com.nhom8.chat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
@IdClass(MessageStatusId.class)
@Entity
@Table(name="message_status",
       indexes = { @Index(name="IX_MSG_STATUS_USER", columnList="user_id,read_at") })
public class MessageStatus {

    @Id
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id", nullable=false)
    private ChatMessage message;

    @Id
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Column(name="delivered_at")
    private Instant deliveredAt;

    @Column(name="read_at")
    private Instant readAt;

    @Column(name="reacted_with", length=16)
    private String reactedWith;

    @Column(name="reacted_at")
    private Instant reactedAt;
}

