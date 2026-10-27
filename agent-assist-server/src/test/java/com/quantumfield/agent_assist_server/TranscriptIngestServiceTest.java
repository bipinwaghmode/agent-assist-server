package com.quantumfield.agent_assist_server;

import com.quantumfield.agent_assist_server.contactlens.ContactLensEvent;
import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.Speaker;
import com.quantumfield.agent_assist_server.model.TranscriptSegment;
import com.quantumfield.agent_assist_server.service.TranscriptIngestService;
import com.quantumfield.agent_assist_server.session.SessionRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptIngestServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateDeliveryDoesNotDuplicateOrRegress() {
        SessionRegistry registry = new SessionRegistry();
        TranscriptIngestService service = new TranscriptIngestService(registry);

        InputStream fixture = getClass().getResourceAsStream("/contact-lens-segments-sample.json");
        assertNotNull(fixture);
        ContactLensEvent event = objectMapper.readValue(fixture, ContactLensEvent.class);

        service.handle(event);
        service.handle(event);   // simulate Kinesis redelivery: partials arrive after the final

        CallSession session = registry.findSession(event.getContactId());
        assertNotNull(session);
        assertEquals(1, session.getSegments().size());

        TranscriptSegment seg = session.getSegments().get(0);
        assertEquals("seg-0004", seg.getSegmentId());
        assertEquals("Sure, it's EMP-4729", seg.getText());
        assertFalse(seg.isPartial());
        assertEquals(Speaker.CUSTOMER, seg.getSpeaker());
        assertEquals(session.getStartedAt().plusMillis(32000), seg.getBeginTime());
    }
}
