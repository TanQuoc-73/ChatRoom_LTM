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
@Table(name = "blocked_user",
       uniqueConstraints = @UniqueConstraint(name="UK_block_pair", columnNames = {"blocker_id","blocked_id"}),
       indexes = {
         @Index(name="IX_BLOCKED_BLOCKER", columnList="blocker_id"),
         @Index(name="IX_BLOCKED_BLOCKED", columnList="blocked_id")
       })
public class BlockedUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="blocker_id", nullable=false)
    private AppUser blocker;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="blocked_id", nullable=false)
    private AppUser blocked;

    @Column(length=500) private String reason;

    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
    // getters/setters
}
