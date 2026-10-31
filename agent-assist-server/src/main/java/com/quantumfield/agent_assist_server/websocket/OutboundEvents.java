package com.quantumfield.agent_assist_server.websocket;

import com.quantumfield.agent_assist_server.model.TranscriptSegment;

public final class OutboundEvents {
    private OutboundEvents() {}

    public record SessionStart(String type, String contactId) {
        public SessionStart(String contactId) { this("session_start", contactId); }
    }
    public record SegmentUpdate(String type, String contactId, TranscriptSegment segment) {
        public SegmentUpdate(String contactId, TranscriptSegment segment) {
            this("segment_update", contactId, segment);
        }
    }
}