package com.imagepipeline.api.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // userId → sesión WebSocket activa
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // roomName → conjunto de userIds (para broadcast a grupos)
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    // ── Gestión de sesiones ─────────────────────────────────────────

    public void register(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
        joinRoom(userId, "feed"); // todos entran al feed global al conectarse
        log.info("Usuario conectado: {}", userId);
    }

    public void remove(String userId) {
        userSessions.remove(userId);
        rooms.values().forEach(room -> room.remove(userId));
        log.info("Usuario desconectado: {}", userId);
    }

    // ── Envío de mensajes ───────────────────────────────────────────

    /** Envía un mensaje a un usuario concreto por su userId. */
    public void sendToUser(String userId, Object payload) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Error enviando mensaje a userId={}: {}", userId, e.getMessage());
            }
        }
    }

    /** Hace broadcast a todos los usuarios de una sala. */
    public void broadcastToRoom(String roomName, Object payload) {
        Set<String> members = rooms.getOrDefault(roomName, Set.of());
        members.forEach(userId -> sendToUser(userId, payload));
    }

    // ── Gestión de salas ────────────────────────────────────────────

    public void joinRoom(String userId, String roomName) {
        rooms.computeIfAbsent(roomName, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void leaveRoom(String userId, String roomName) {
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).remove(userId);
        }
    }

    public boolean isConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
