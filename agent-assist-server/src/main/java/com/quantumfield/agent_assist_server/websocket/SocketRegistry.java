package com.quantumfield.agent_assist_server.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SocketRegistry {
    private final Map<String, WebSocketSession> agentIdToSession = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdToAgentId = new ConcurrentHashMap<>();

    public Optional<WebSocketSession> findSocket(String agentId) {
        return Optional.ofNullable(agentIdToSession.get(agentId))
                .filter(WebSocketSession::isOpen);
    }

    public void register(String agentId, WebSocketSession session) {
        log.info("Registering: {}", agentId);
        sessionIdToAgentId.put(session.getId(), agentId);
        WebSocketSession old = agentIdToSession.put(agentId, session);
        if (old != null && old != session) {
            sessionIdToAgentId.remove(old.getId());
            closeQuietly(old);
        }
        log.info("Registration completed for: {}", agentId);
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(new CloseStatus(4000, "Replaced by a newer connection"));
        } catch (IOException e) {
            log.warn("Failed to close replaced session {}", session.getId(), e);
        }
    }

    public void unregister(WebSocketSession session) {
        String agentId = sessionIdToAgentId.remove(session.getId());   // remove returns the value
        log.info("Unregistering: {}", agentId);
        if (agentId != null) {
            agentIdToSession.remove(agentId, session);                 // only if still this session
        }
        log.info("Unregistration completed for: {}", agentId);
    }
}
