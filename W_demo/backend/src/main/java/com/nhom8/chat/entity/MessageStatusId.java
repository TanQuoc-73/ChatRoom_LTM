package com.nhom8.chat.entity;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusId implements Serializable {
    private Long message;
    private Long user;

    @Override
    public int hashCode() {
        return Objects.hash(message, user);
    }
}
