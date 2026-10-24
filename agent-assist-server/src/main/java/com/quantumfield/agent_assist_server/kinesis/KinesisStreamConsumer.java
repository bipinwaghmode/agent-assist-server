package com.quantumfield.agent_assist_server.kinesis;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KinesisStreamConsumer {

    private final KinesisProperties properties;

    private Scheduler scheduler;

    KinesisAsyncClient kinesisClient;
    DynamoDbAsyncClient dynamoClient;
    CloudWatchAsyncClient cloudWatchClient;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Starting Kinesis Stream Consumer for stream: {}", properties.getStreamName());

        // 1. Choose credentials provider
        AwsCredentialsProvider credentialsProvider = (properties.getEndpoint() != null && !properties.getEndpoint().isBlank())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                : DefaultCredentialsProvider.create();

        Region region = Region.of(properties.getRegion());

        // 2. Build Async Clients
        KinesisAsyncClientBuilder kinesisBuilder = KinesisAsyncClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider);

        DynamoDbAsyncClientBuilder dynamoBuilder = DynamoDbAsyncClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider);

        CloudWatchAsyncClientBuilder cloudWatchBuilder = CloudWatchAsyncClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider);

        // Apply endpoint overrides if present
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            URI endpointUri = URI.create(properties.getEndpoint());
            kinesisBuilder.endpointOverride(endpointUri);
            dynamoBuilder.endpointOverride(endpointUri);
            cloudWatchBuilder.endpointOverride(endpointUri);
        }

        // Kinesis specific builder utility wrapper
        kinesisClient = software.amazon.kinesis.common.KinesisClientUtil.createKinesisAsyncClient(kinesisBuilder);
        dynamoClient = dynamoBuilder.build();
        cloudWatchClient = cloudWatchBuilder.build();

        // 3. Create ConfigsBuilder
        String workerId = properties.getApplicationName() + "(worker)-" + UUID.randomUUID();

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
                properties.getStreamName(),
                properties.getApplicationName(),
                kinesisClient,
                dynamoClient,
                cloudWatchClient,
                workerId,
                () -> new KinesisShardRecordProcessor()
        );

        // 4. Configure and create the Scheduler
        this.scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig().initialPositionInStream(
                        InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.TRIM_HORIZON)
                ),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig().retrievalSpecificConfig(
                        new PollingConfig(properties.getStreamName(), kinesisClient)
                )
        );

        // 5. Run the scheduler in a daemon thread
        Thread t = new Thread(scheduler, "kcl-scheduler");
        t.setDaemon(true);
        t.start();

        log.info("KCL Scheduler started successfully with workerId: {}", workerId);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            log.info("Initiating graceful shutdown of KCL Scheduler...");
            try {
                Future<Boolean> shutdownFuture = scheduler.startGracefulShutdown();

                Boolean cleanShutdown = shutdownFuture.get(20, TimeUnit.SECONDS);
                kinesisClient.close();
                dynamoClient.close();
                cloudWatchClient.close();
                log.info("KCL Scheduler shutdown complete. Clean shutdown: {}", cleanShutdown);
            } catch (Exception e) {
                log.error("Error or timeout during Kinesis scheduler graceful shutdown", e);
            }
        }
    }
}
