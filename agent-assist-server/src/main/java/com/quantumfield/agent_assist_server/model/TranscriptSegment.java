package com.quantumfield.agent_assist_server.model;

import lombok.Data;

import java.time.Instant;

@Data
public class TranscriptSegment {
    private boolean isPartial;
    private String segmentId;
    private String text;
    private Instant beginTime;
    private Instant endTime;
    private String sentiment;
}
