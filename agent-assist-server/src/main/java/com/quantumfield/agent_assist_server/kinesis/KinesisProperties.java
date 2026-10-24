package com.quantumfield.agent_assist_server.kinesis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "contactlens.kinesis")
public class KinesisProperties {
    private String streamName;
    private String applicationName;
    private String region = "us-east-1";
    private String endpoint;   // blank means real AWS
}