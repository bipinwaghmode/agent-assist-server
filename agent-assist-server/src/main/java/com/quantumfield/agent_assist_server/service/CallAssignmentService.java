package com.quantumfield.agent_assist_server.service;

import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.session.SessionRegistry;
import com.quantumfield.agent_assist_server.websocket.TranscriptPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallAssignmentService {

    private final SessionRegistry sessionRegistry;
    private final TranscriptPublisher publisher;

    public CallSession assign(String contactId, String agentId) {
        CallSession session = sessionRegistry.getOrCreate(contactId);
        synchronized (session) {
            session.setAgentId(agentId);
            publisher.replay(session);   // sends everything buffered so far
        }
        log.info("Assigned contact {} to agent {}", contactId, agentId);
        return session;
    }
}
