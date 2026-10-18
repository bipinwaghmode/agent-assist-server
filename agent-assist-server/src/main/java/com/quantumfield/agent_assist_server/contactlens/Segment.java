package com.quantumfield.agent_assist_server.contactlens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Segment {

    @JsonProperty("Utterance")
    private UtteranceSegment utterance;

    @JsonProperty("Transcript")
    private TranscriptSegmentRaw transcript;
}
