package com.agrochain.backend.config;

import com.agrochain.backend.service.JwtService;
import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import java.util.Map;

/**
 * Socket.IO chat transport — replaces the STOMP WebSocket path for live message
 * delivery. Our carrier hotspot silently drops raw WebSocket frames after the
 * upgrade handshake (confirmed: REST always works, STOMP CONNECT frames never
 * arrive over that network); Socket.IO's polling fallback survives networks like
 * that because it degrades to plain HTTP requests when the WebSocket transport
 * doesn't get traffic through.
 *
 * Two independent auth paths are supported since different Socket.IO client
 * configurations put the token in different places:
 *   1. Query param (?token=...) — checked in the AuthorizationListener, which
 *      runs for every connection attempt regardless of transport.
 *   2. The v4 client's `auth: { token }` payload — checked in the
 *      AuthTokenListener, which only fires if the client actually sends one.
 * Because path 2 is optional at the protocol level, the ConnectListener acts as
 * a safety net: any client that reaches CONNECTED without an email resolved by
 * either path gets disconnected immediately.
 */
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
@Slf4j
public class SocketIOConfig {

    private static final int PORT = 9092;

    private final JwtService jwtService;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(PORT);
        config.setOrigin("*");
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        config.setAuthorizationListener(this::authorize);
        // netty-socketio uses its own standalone Jackson ObjectMapper, separate from
        // Spring's auto-configured one — without this it silently fails to serialize
        // any LocalDateTime field (e.g. ChatMessageResponse.createdAt), which drops
        // the whole broadcast packet before it reaches any client. Also disable
        // timestamp-array output so createdAt matches the ISO-8601 string format the
        // REST endpoints already return, instead of a [y,m,d,h,m,s,nanos] array.
        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()) {
            @Override
            protected void init(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
                super.init(objectMapper);
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        });

        server = new SocketIOServer(config);
        server.addNamespace(Namespace.DEFAULT_NAME).addAuthTokenListener(this::authorizeToken);
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(this::onDisconnect);

        return server;
    }

    // Starts only after every other bean (including SocketIOChatModule's event
    // handler registration) has finished initializing, so the server never
    // accepts a connection before its event listeners exist.
    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        server.start();
        log.info("Socket.IO server started on 0.0.0.0:{} (transports: WEBSOCKET, POLLING)", PORT);
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.stop();
            log.info("Socket.IO server stopped");
        }
    }

    private AuthorizationResult authorize(HandshakeData data) {
        String token = data.getSingleUrlParam("token");
        if (token == null) {
            // No query-param token — don't hard-reject here. The v4 auth-payload
            // path or the post-connect safety check makes the final call.
            log.info("Socket.IO handshake with no query-param token from {} — deferring to auth-payload check", data.getAddress());
            return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
        }
        if (!jwtService.isTokenValid(token)) {
            log.warn("Socket.IO handshake rejected — invalid query-param token from {}", data.getAddress());
            return AuthorizationResult.FAILED_AUTHORIZATION;
        }
        String email = jwtService.extractEmail(token);
        log.info("Socket.IO handshake accepted for {} (query param)", email);
        return new AuthorizationResult(true, Map.of("email", email));
    }

    private AuthTokenResult authorizeToken(Object authToken, SocketIOClient client) {
        if (client.has("email")) {
            // Already authenticated via the query-param path above.
            return AuthTokenResult.AuthTokenResultSuccess;
        }
        String token = extractToken(authToken);
        if (token == null || !jwtService.isTokenValid(token)) {
            log.warn("Socket.IO auth-payload token rejected for session {}", client.getSessionId());
            return new AuthTokenResult(false, "Invalid or missing token");
        }
        String email = jwtService.extractEmail(token);
        client.set("email", email);
        log.info("Socket.IO handshake accepted for {} (auth payload)", email);
        return AuthTokenResult.AuthTokenResultSuccess;
    }

    private String extractToken(Object authToken) {
        if (authToken instanceof String s) {
            return s;
        }
        if (authToken instanceof Map<?, ?> map) {
            Object t = map.get("token");
            return t != null ? t.toString() : null;
        }
        return null;
    }

    private void onConnect(SocketIOClient client) {
        if (!client.has("email")) {
            log.warn("Socket.IO client {} reached CONNECTED with no token validated on either path — disconnecting", client.getSessionId());
            client.disconnect();
            return;
        }
        log.info("Socket.IO client connected: session={}, email={}, transport={}",
                client.getSessionId(), client.get("email"), client.getTransport());
    }

    private void onDisconnect(SocketIOClient client) {
        log.info("Socket.IO client disconnected: session={}, email={}", client.getSessionId(), client.get("email"));
    }
}
