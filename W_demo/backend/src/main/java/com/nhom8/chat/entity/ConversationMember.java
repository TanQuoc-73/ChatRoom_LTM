package com.nhom8.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="conversation_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

    @EmbeddedId
    private ConversationMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(length = 20, nullable = false)
    private String role = "MEMBER";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="last_read_message_id")
    private ChatMessage lastReadMessage;

    @Column(name="is_muted", nullable = false)
    private boolean muted = false;

    @Column(length = 100)
    private String nickname;
}
