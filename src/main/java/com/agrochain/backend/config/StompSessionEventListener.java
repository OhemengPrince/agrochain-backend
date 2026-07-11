package com.agrochain.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@Slf4j
public class StompSessionEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("STOMP CONNECT frame received — sessionId={}, user={}, heart-beat={}",
                accessor.getSessionId(), event.getUser(), accessor.getHeader("heart-beat"));
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("STOMP CONNECTED sent to client — sessionId={}", accessor.getSessionId());
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("STOMP SUBSCRIBE — sessionId={}, destination={}", accessor.getSessionId(), accessor.getDestination());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("STOMP session disconnected — sessionId={}, closeStatus={}", event.getSessionId(), event.getCloseStatus());
    }
}
