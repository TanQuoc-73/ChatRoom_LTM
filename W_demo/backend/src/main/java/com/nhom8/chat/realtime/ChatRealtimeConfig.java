package com.nhom8.chat.realtime;

import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.ConversationRepository;
import com.nhom8.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatRealtimeConfig {

    private final AppUserRepository userRepo;
    private final ConversationRepository convRepo;
    private final ConversationService conversationService;

    @Bean
    public AuthGateway authGateway() {
        return new DbAuthGateway(userRepo, conversationService);
    }

    @Bean
    public MessageStore messageStore() {
        return new InMemoryMessageStoreImpl();
    }

    @Bean
    public RoomStore roomStore() {
        return new DbRoomStore(convRepo, conversationService);
    }
}
