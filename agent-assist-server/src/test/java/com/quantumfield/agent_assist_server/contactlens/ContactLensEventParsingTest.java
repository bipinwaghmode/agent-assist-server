package com.quantumfield.agent_assist_server.contactlens;

import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.service.TranscriptIngestService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContactLensEventParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesSegmentsEventsFromFixture(){
        InputStream fixture = getClass().getResourceAsStream("/contact-lens-segments-sample.json");
        assertNotNull(fixture, "contact-lens-segments-sample.json not found at src/test/resources");

        ContactLensEvent event = objectMapper.readValue(fixture, ContactLensEvent.class);

        TranscriptIngestService transcriptIngestService = new TranscriptIngestService();
        CallSession callSession = new CallSession();
        List<Segment> segments = event.getSegments();

        transcriptIngestService.storeUtteranceSegment(callSession, segments.get(0).getUtterance());
        String firstUtteranceTranscriptId = callSession.getSegments().get(0).getSegmentId();
        transcriptIngestService.storeUtteranceSegment(callSession, segments.get(1).getUtterance());
        transcriptIngestService.storeTranscriptSegment(callSession, segments.get(2).getTranscript());

        assertEquals("seg-0004", callSession.getSegments().get(0).getSegmentId());
        assertEquals(1, callSession.getSegments().size());
        assertEquals("Sure, it's EMP-4729", callSession.getSegments().get(0).getText());
        assertEquals(false, callSession.getSegments().get(0).isPartial());
        assertEquals(firstUtteranceTranscriptId, callSession.getSegments().get(0).getSegmentId());

        /*assertEquals("SEGMENTS", event.getEventType());

        List<Segment> segments = event.getSegments();

        assertEquals(3, segments.size());

        assertNotNull(segments.get(0).getUtterance());
        assertNull(segments.get(0).getTranscript());
        assertEquals("seg-0004", segments.get(0).getUtterance().getTranscriptId());

        assertNull(segments.get(2).getUtterance());
        assertNotNull(segments.get(2).getTranscript());
        assertEquals("seg-0004", segments.get(2).getTranscript().getId());

        assertEquals("NEUTRAL", segments.get(2).getTranscript().getSentiment());*/

    }
}
