package com.nhom8.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// cấu hình websocket và stomp cho ứng dụng
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // cấu hình message broker
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // cấu hình broker đơn giản với prefix /topic
        config.enableSimpleBroker("/topic");
        // prefix cho các message gửi từ client tới server
        config.setApplicationDestinationPrefixes("/app");
    }

    // đăng ký các endpoint websocket
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // endpoint websocket hỗ trợ sockjs
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // endpoint websocket thuần không dùng sockjs
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*"); 
    }
}
