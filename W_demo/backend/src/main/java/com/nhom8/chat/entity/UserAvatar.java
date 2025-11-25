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

@Table(
    name = "user_avatar",
    uniqueConstraints = @UniqueConstraint(
        name = "UX_user_avatar_current",
        columnNames = {"user_id"}
    )
)
public class UserAvatar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="media_id", nullable=false)
    private Media media;

    @Column(name="is_current", nullable=false)
    private boolean current = false;

    @Column(name="set_as_avatar_at", nullable=false)
    private Instant setAsAvatarAt = Instant.now();

}
