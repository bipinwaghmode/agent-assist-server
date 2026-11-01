package com.quantumfield.agent_assist_server.session;

import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.SessionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {
    private final Map<String, CallSession> contactIdToSession = new ConcurrentHashMap<>();

    public CallSession findSession(String contactId) {
        return contactIdToSession.get(contactId);
    }

    public void addSession(CallSession session) {
        contactIdToSession.putIfAbsent(session.getContactId(), session);
    }

    public void removeSession(String contactId) {
        contactIdToSession.remove(contactId);
    }

    public CallSession getOrCreate(String contactId) {
        return contactIdToSession.computeIfAbsent(contactId, id -> {
            CallSession s = new CallSession();
            s.setContactId(id);
            s.setStartedAt(Instant.now());   // approximation: when we first saw the contact
            return s;
        });
    }

    public Optional<CallSession> findByAgentId(String agentId) {
        return contactIdToSession.values().stream()
                .filter(s -> agentId.equals(s.getAgentId()))
                .max(Comparator.comparing(CallSession::getStartedAt));
    }

    public int evictEndedBefore(Instant cutoff) {
        int before = contactIdToSession.size();
        contactIdToSession.values().removeIf(s ->
                s.getStatus() == SessionStatus.ENDED
                        && s.getEndedAt() != null
                        && s.getEndedAt().isBefore(cutoff));
        return before - contactIdToSession.size();
    }
}
