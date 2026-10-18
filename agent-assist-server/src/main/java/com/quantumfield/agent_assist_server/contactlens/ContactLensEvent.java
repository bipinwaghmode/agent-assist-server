package com.quantumfield.agent_assist_server.contactlens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactLensEvent {

    @JsonProperty("Version")
    private String version;

    @JsonProperty("Channel")
    private String channel;

    @JsonProperty("AccountId")
    private String accountId;

    @JsonProperty("InstanceId")
    private String instanceId;

    @JsonProperty("ContactId")
    private String contactId;

    @JsonProperty("LanguageCode")
    private String languageCode;

    @JsonProperty("EventType")
    private String eventType;

    @JsonProperty("Segments")
    private List<Segment> segments;
}
