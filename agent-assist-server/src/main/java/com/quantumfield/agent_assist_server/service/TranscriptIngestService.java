package com.quantumfield.agent_assist_server.service;

import com.quantumfield.agent_assist_server.contactlens.ContactLensEvent;
import com.quantumfield.agent_assist_server.contactlens.Segment;
import com.quantumfield.agent_assist_server.contactlens.TranscriptSegmentRaw;
import com.quantumfield.agent_assist_server.contactlens.UtteranceSegment;
import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.SessionStatus;
import com.quantumfield.agent_assist_server.model.Speaker;
import com.quantumfield.agent_assist_server.model.TranscriptSegment;
import com.quantumfield.agent_assist_server.session.SessionRegistry;
import com.quantumfield.agent_assist_server.websocket.TranscriptPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptIngestService {

    private final SessionRegistry sessionRegistry;
    private final TranscriptPublisher publisher;

    public void handle(ContactLensEvent event) {
        if (event.getContactId() == null || event.getEventType() == null) {
            log.warn("Ignoring event without ContactId/EventType");
            return;
        }
        if ("SEGMENTS".equals(event.getEventType())) {
            handleSegments(event);
        } else if("COMPLETED".equals(event.getEventType()) || "FAILED".equals(event.getEventType())){
            handleEnded(event);
        }else {
            log.debug("Ignoring event type {} for contact {}", event.getEventType(), event.getContactId());
        }
    }

    private void handleSegments(ContactLensEvent event) {
        if (event.getSegments() == null) return;
        CallSession session = sessionRegistry.getOrCreate(event.getContactId());
        synchronized (session) {
            for (Segment segment : event.getSegments()) {
                TranscriptSegment stored = null;
                if (segment.getUtterance() != null) stored = storeUtteranceSegment(session, segment.getUtterance());
                else if (segment.getTranscript() != null) stored = storeTranscriptSegment(session, segment.getTranscript());
                if (stored != null) publisher.publishSegment(session, stored);
            }
        }
        log.info("segments={}", session.getSegments());
    }

    private void handleEnded(ContactLensEvent event) {
        CallSession session = sessionRegistry.findSession(event.getContactId());
        if (session == null) return;                       // never saw this call
        synchronized (session) {
            if (session.getStatus() == SessionStatus.ENDED) return;   // duplicate event (redelivery)
            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(Instant.now());
            publisher.publishSessionEnd(session);
        }
    }

    public TranscriptSegment storeUtteranceSegment(CallSession session, UtteranceSegment u) {
        TranscriptSegment seg = new TranscriptSegment();
        seg.setPartial(true);
        seg.setSegmentId(u.getTranscriptId());
        seg.setSpeaker(toSpeaker(u.getParticipantRole()));
        seg.setText(u.getPartialContent());
        seg.setBeginTime(session.getStartedAt().plusMillis(u.getBeginOffsetMillis()));
        return upsert(session, seg);
    }

    public TranscriptSegment storeTranscriptSegment(CallSession session, TranscriptSegmentRaw t) {
        TranscriptSegment seg = new TranscriptSegment();
        seg.setPartial(false);
        seg.setSegmentId(t.getId());
        seg.setSpeaker(toSpeaker(t.getParticipantRole()));
        seg.setText(t.getContent());
        seg.setBeginTime(session.getStartedAt().plusMillis(t.getBeginOffsetMillis()));
        seg.setEndTime(session.getStartedAt().plusMillis(t.getEndOffsetMillis()));
        seg.setSentiment(t.getSentiment());
        return upsert(session, seg);
    }

    private TranscriptSegment upsert(CallSession session, TranscriptSegment incoming) {
        TranscriptSegment existing = session.findSegment(incoming.getSegmentId());
        if (existing == null) { session.addSegment(incoming); return incoming; }
        if (incoming.isPartial() && !existing.isPartial()) { return null; }   // ignored
        session.updateSegment(incoming.getSegmentId(), incoming);
        return incoming;
    }

    private Speaker toSpeaker(String role) {
        if ("AGENT".equalsIgnoreCase(role)) return Speaker.AGENT;
        if ("CUSTOMER".equalsIgnoreCase(role)) return Speaker.CUSTOMER;
        log.warn("Unexpected ParticipantRole '{}', treating as CUSTOMER", role);
        return Speaker.CUSTOMER;
    }
}
