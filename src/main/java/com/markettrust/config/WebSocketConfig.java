package com.markettrust.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket/STOMP configuration for real-time messaging features:
 * <ul>
 *   <li>/topic/** — broadcast (one-to-many)</li>
 *   <li>/queue/** — point-to-point queues</li>
 *   <li>/user/**  — per-user destinations</li>
 *   <li>/app/**   — client→server destination prefix</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for topic broadcasts and user queues
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for @MessageMapping methods on the server side
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destination routing (e.g. /user/{sessionId}/queue/errors)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();   // fallback for browsers that don't support native WebSocket

        // Native WebSocket endpoint (for clients that support it, e.g. mobile)
        registry.addEndpoint("/ws-native")
                .setAllowedOrigins(allowedOrigins);
    }
}
