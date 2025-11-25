package com.nhom8.chat.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberId implements Serializable {
    private Long conversationId;
    private Long userId;

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
