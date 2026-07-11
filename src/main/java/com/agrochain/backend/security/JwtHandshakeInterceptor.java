package com.agrochain.backend.security;

import com.agrochain.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.info("WS handshake attempt from {} — uri={}", request.getRemoteAddress(), request.getURI());

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WS handshake rejected: request is not a ServletServerHttpRequest");
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null) {
            log.warn("WS handshake rejected: no 'token' query parameter present");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!jwtService.isTokenValid(token)) {
            log.warn("WS handshake rejected: token failed validation");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        String email = jwtService.extractEmail(token);
        attributes.put("email", email);
        log.info("WS handshake accepted for {}", email);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("WS handshake completed with exception", exception);
        } else {
            log.info("WS handshake completed successfully");
        }
    }
}
