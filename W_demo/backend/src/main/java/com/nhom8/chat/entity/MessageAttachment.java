package com.nhom8.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "message_attachment")
public class MessageAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id", nullable=false)
    private ChatMessage message;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="media_id", nullable=false)
    private Media media;

    @Column(length=500) private String caption;

    @Column(name="sort_order", nullable=false)
    private int sortOrder = 0;
   
}
