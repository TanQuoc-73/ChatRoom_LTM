package com.nhom8.chat.entity;

import com.nhom8.chat.entity.enums.MessageKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20, nullable = false)
    private MessageKind messageType;

    @Lob
    private String content;

    @Column(name = "client_cid", length = 128)
    private String clientCid;

    @Column(name = "is_edited", nullable = false)
    private boolean edited;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MessageAttachment> attachments;
}