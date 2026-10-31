package com.quantumfield.agent_assist_server.websocket;

import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.TranscriptSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptPublisher {
    private final SocketRegistry socketRegistry;
    private final ObjectMapper objectMapper;   // tools.jackson.databind

    public void publishSegment(CallSession session, TranscriptSegment seg) {
        if (session.getAgentId() == null) return;   // buffered in the session; replayed on assignment
        send(session.getAgentId(), new OutboundEvents.SegmentUpdate(session.getContactId(), seg));
    }

    public void replay(CallSession session) {
        if (session.getAgentId() == null) return;
        synchronized (session) {   // same lock as ingest, so a replay can't interleave with live updates
            send(session.getAgentId(), new OutboundEvents.SessionStart(session.getContactId()));
            for (TranscriptSegment seg : session.getSegments()) {
                send(session.getAgentId(), new OutboundEvents.SegmentUpdate(session.getContactId(), seg));
            }
        }
    }

    private void send(String agentId, Object payload) {
        socketRegistry.findSocket(agentId).ifPresent(ws -> {
            try {
                String json = objectMapper.writeValueAsString(payload);
                synchronized (ws) { ws.sendMessage(new TextMessage(json)); }
            } catch (Exception e) {
                log.warn("Push to agent {} failed", agentId, e);
            }
        });
    }
}