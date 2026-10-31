package com.quantumfield.agent_assist_server.websocket;

import com.quantumfield.agent_assist_server.session.SessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class AgentSocketHandler extends TextWebSocketHandler {
    private final SocketRegistry socketRegistry;
    private final SessionRegistry sessionRegistry;
    private final TranscriptPublisher publisher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String agentId = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("agentId");
        if(agentId != null) {
            socketRegistry.register(agentId, session);
            sessionRegistry.findByAgentId(agentId).ifPresent(publisher::replay);
        }
        else
            session.close(CloseStatus.POLICY_VIOLATION);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,CloseStatus status){
        socketRegistry.unregister(session);
    }
}
