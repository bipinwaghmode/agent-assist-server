package com.quantumfield.agent_assist_server.kinesis;

import com.quantumfield.agent_assist_server.contactlens.ContactLensEvent;
import com.quantumfield.agent_assist_server.service.TranscriptIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class KinesisShardRecordProcessor implements ShardRecordProcessor {

    private static final int CHECKPOINT_EVERY_RECORDS = 50;
    private static final Duration CHECKPOINT_EVERY = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;            // tools.jackson.databind.ObjectMapper
    private final TranscriptIngestService ingestService;

    private String shardId;
    private long recordsSinceCheckpoint = 0;
    private Instant lastCheckpointTime = Instant.now();

    @Override
    public void initialize(InitializationInput input) {
        this.shardId = input.shardId();
        log.info("Initialized processor for shard: {}", shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput input) {
        for (KinesisClientRecord record : input.records()) {
            try {
                String json = StandardCharsets.UTF_8.decode(record.data()).toString();
                ContactLensEvent event = objectMapper.readValue(json, ContactLensEvent.class);
                ingestService.handle(event);
            } catch (Exception e) {
                log.error("Failed to process record seq={} on shard {}", record.sequenceNumber(), shardId, e);
            }
        }
        recordsSinceCheckpoint += input.records().size();

        boolean dueByCount = recordsSinceCheckpoint >= CHECKPOINT_EVERY_RECORDS;
        boolean dueByTime = recordsSinceCheckpoint > 0
                && Duration.between(lastCheckpointTime, Instant.now()).compareTo(CHECKPOINT_EVERY) >= 0;
        if (dueByCount || dueByTime) {
            checkpoint(input.checkpointer());
        }
    }

    @Override
    public void leaseLost(LeaseLostInput input) {
        log.info("Lost lease for shard: {}", shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput input) {
        checkpoint(input.checkpointer());
        log.info("Shard ended: {}", shardId);
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput input) {
        checkpoint(input.checkpointer());
        log.info("Shutdown requested for shard: {}", shardId);
    }

    private void checkpoint(RecordProcessorCheckpointer checkpointer) {
        try {
            checkpointer.checkpoint();
            recordsSinceCheckpoint = 0;
            lastCheckpointTime = Instant.now();
        } catch (Exception e) {
            log.error("Checkpoint failed on shard {}", shardId, e);
        }
    }
}