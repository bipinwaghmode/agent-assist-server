package com.quantumfield.agent_assist_server.service;

import com.quantumfield.agent_assist_server.contactlens.ContactLensEvent;
import com.quantumfield.agent_assist_server.contactlens.Segment;
import com.quantumfield.agent_assist_server.contactlens.TranscriptSegmentRaw;
import com.quantumfield.agent_assist_server.contactlens.UtteranceSegment;
import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.Speaker;
import com.quantumfield.agent_assist_server.model.TranscriptSegment;
import com.quantumfield.agent_assist_server.session.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptIngestService {

    private final SessionRegistry sessionRegistry;

    public void handle(ContactLensEvent event) {
        if (event.getContactId() == null || event.getEventType() == null) {
            log.warn("Ignoring event without ContactId/EventType");
            return;
        }
        if ("SEGMENTS".equals(event.getEventType())) {
            handleSegments(event);
        } else {
            log.debug("Ignoring event type {} for contact {}", event.getEventType(), event.getContactId());
        }
    }

    private void handleSegments(ContactLensEvent event) {
        if (event.getSegments() == null) return;
        CallSession session = sessionRegistry.getOrCreate(event.getContactId());
        for (Segment segment : event.getSegments()) {
            if (segment.getUtterance() != null) {
                storeUtteranceSegment(session, segment.getUtterance());
            } else if (segment.getTranscript() != null) {
                storeTranscriptSegment(session, segment.getTranscript());
            }
        }
    }

    public void storeUtteranceSegment(CallSession session, UtteranceSegment u) {
        TranscriptSegment seg = new TranscriptSegment();
        seg.setPartial(true);
        seg.setSegmentId(u.getTranscriptId());
        seg.setSpeaker(toSpeaker(u.getParticipantRole()));
        seg.setText(u.getPartialContent());
        seg.setBeginTime(session.getStartedAt().plusMillis(u.getBeginOffsetMillis()));
        upsert(session, seg);
    }

    public void storeTranscriptSegment(CallSession session, TranscriptSegmentRaw t) {
        TranscriptSegment seg = new TranscriptSegment();
        seg.setPartial(false);
        seg.setSegmentId(t.getId());
        seg.setSpeaker(toSpeaker(t.getParticipantRole()));
        seg.setText(t.getContent());
        seg.setBeginTime(session.getStartedAt().plusMillis(t.getBeginOffsetMillis()));
        seg.setEndTime(session.getStartedAt().plusMillis(t.getEndOffsetMillis()));
        seg.setSentiment(t.getSentiment());
        upsert(session, seg);
    }

    private void upsert(CallSession session, TranscriptSegment incoming) {
        TranscriptSegment existing = session.findSegment(incoming.getSegmentId());
        if (existing == null) {
            session.addSegment(incoming);
        } else if (incoming.isPartial() && !existing.isPartial()) {
            log.debug("Ignoring partial for already-final segment {}", incoming.getSegmentId());
        } else {
            session.updateSegment(incoming.getSegmentId(), incoming);
        }
    }

    private Speaker toSpeaker(String role) {
        if ("AGENT".equalsIgnoreCase(role)) return Speaker.AGENT;
        if ("CUSTOMER".equalsIgnoreCase(role)) return Speaker.CUSTOMER;
        log.warn("Unexpected ParticipantRole '{}', treating as CUSTOMER", role);
        return Speaker.CUSTOMER;
    }
}
