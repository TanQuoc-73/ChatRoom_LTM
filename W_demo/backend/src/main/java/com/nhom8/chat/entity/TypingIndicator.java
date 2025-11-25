package com.nhom8.chat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "typing_indicator",
       uniqueConstraints = @UniqueConstraint(name="UX_typing_unique", columnNames = {"conversation_id","user_id"}))
public class TypingIndicator {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conversation_id", nullable=false)
    private Conversation conversation;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Column(name="started_at", nullable=false) private Instant startedAt = Instant.now();
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    // getters/setters
}
