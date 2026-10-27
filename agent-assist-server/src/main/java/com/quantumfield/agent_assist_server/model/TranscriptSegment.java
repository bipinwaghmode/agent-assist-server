package com.quantumfield.agent_assist_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class TranscriptSegment {
    private Speaker speaker;
    @JsonProperty("isPartial")
    private boolean partial;
    private String segmentId;
    private String text;
    private Instant beginTime;
    private Instant endTime;
    private String sentiment;
}
