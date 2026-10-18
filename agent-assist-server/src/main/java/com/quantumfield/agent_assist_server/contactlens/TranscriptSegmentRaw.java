package com.quantumfield.agent_assist_server.contactlens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptSegmentRaw {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("ParticipantId")
    private String participantId;

    @JsonProperty("ParticipantRole")
    private String participantRole;

    @JsonProperty("Content")
    private String content;

    @JsonProperty("BeginOffsetMillis")
    private long beginOffsetMillis;

    @JsonProperty("EndOffsetMillis")
    private long endOffsetMillis;

    @JsonProperty("Sentiment")
    private String sentiment;
}
