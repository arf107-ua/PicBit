package com.imagepipeline.api.socket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final SessionRegistry sessionRegistry;

    /**
     * Se llama cuando un cliente establece la conexión WS.
     * El userId se extrae del query param: ws://host/ws?userId=xxx
     * En producción esto debería ser un JWT validado.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        if (userId != null) {
            sessionRegistry.register(userId, session);
            // Confirmar conexión al cliente
            sessionRegistry.sendToUser(userId, Map.of(
                "type", "connected",
                "message", "Conectado correctamente"
            ));
        } else {
            log.warn("Conexión WS sin userId, cerrando...");
            try { session.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Se llama cuando el cliente envía un mensaje al servidor por WS.
     * Útil para que el cliente se una a salas o envíe heartbeats.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = extractUserId(session);
        log.debug("Mensaje WS recibido de {}: {}", userId, message.getPayload());
        // Aquí puedes procesar mensajes del cliente si lo necesitas
    }

    /**
     * Se llama cuando la conexión se cierra.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session);
        if (userId != null) {
            sessionRegistry.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String userId = extractUserId(session);
        log.error("Error WS para userId={}: {}", userId, exception.getMessage());
        if (userId != null) {
            sessionRegistry.remove(userId);
        }
    }

    // ── Helper ──────────────────────────────────────────────────────

    private String extractUserId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "userId".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
}
