package com.quantumfield.agent_assist_server.session;

import com.quantumfield.agent_assist_server.model.CallSession;
import org.springframework.stereotype.Component;

import java.util.Map;
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
}
