package com.quantumfield.agent_assist_server.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanup {
    private static final Duration RETENTION = Duration.ofMinutes(10);
    private final SessionRegistry sessionRegistry;

    @Scheduled(fixedDelay = 60_000)   // every minute, measured from the end of the previous run
    public void evictEndedSessions() {
        int removed = sessionRegistry.evictEndedBefore(Instant.now().minus(RETENTION));
        if (removed > 0) log.info("Evicted {} ended session(s)", removed);
    }
}
