package com.quantumfield.agent_assist_server.contactlens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UtteranceSegment {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("TranscriptId")
    private String transcriptId;

    @JsonProperty("ParticipantId")
    private String participantId;

    @JsonProperty("ParticipantRole")
    private String participantRole;

    @JsonProperty("PartialContent")
    private String partialContent;

    @JsonProperty("BeginOffsetMillis")
    private long beginOffsetMillis;

    @JsonProperty("EndOffsetMillis")
    private long endOffsetMillis;
}
