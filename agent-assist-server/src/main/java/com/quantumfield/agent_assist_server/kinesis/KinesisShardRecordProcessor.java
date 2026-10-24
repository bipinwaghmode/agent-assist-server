package com.quantumfield.agent_assist_server.kinesis;

import lombok.extern.slf4j.Slf4j;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
public class KinesisShardRecordProcessor implements ShardRecordProcessor {
    private String shardId;
    private long recordsProcessed = 0;
    private long recordsSinceLastCheckpoint = 0;
    private Instant lastCheckpointTime = Instant.now();

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.shardId();
        log.info("Initialized processor for shard: {}", shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        for (KinesisClientRecord record : processRecordsInput.records()) {
            try {
                String json = StandardCharsets.UTF_8.decode(record.data()).toString();
                log.info("shard={} seq={} payload={}", shardId, record.sequenceNumber(), json);
                recordsProcessed++;
                recordsSinceLastCheckpoint++;
            } catch (Exception e) {
                log.error("Error processing record seq={}", record.sequenceNumber(), e);
            }
        }

        if (recordsSinceLastCheckpoint>=50 ||
                (recordsSinceLastCheckpoint > 0 &&
                        Instant.now().isAfter(lastCheckpointTime.plusSeconds(60)))) {
            try {
                processRecordsInput.checkpointer().checkpoint();
                lastCheckpointTime = Instant.now();
                recordsSinceLastCheckpoint = 0;
                log.info("Checkpointed shard: {} after {} records.", shardId, recordsProcessed);
            } catch (Exception e) {
                log.error("Checkpoint failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.info("Lost lease for shard: {}", shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        try {
            shardEndedInput.checkpointer().checkpoint();
            log.info("Shard ended: {}", shardId);
        } catch (Exception e) {
            log.error("Checkpoint on shard end failed: {}", e.getMessage());
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        try {
            shutdownRequestedInput.checkpointer().checkpoint();
            log.info("Shutdown requested for shard: {}", shardId);
        } catch (Exception e) {
            log.error("Checkpoint on shutdown failed: {}", e.getMessage());
        }
    }
}
