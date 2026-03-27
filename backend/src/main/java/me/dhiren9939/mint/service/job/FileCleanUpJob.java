package me.dhiren9939.mint.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanUpJob {

    private final FileCleanUpWorker fileCleanUpWorker;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanUp() {
        log.info("Starting cleanup job...");

        boolean hasMore = true;
        while (hasMore) {
            hasMore = fileCleanUpWorker.processBatchCleanUp();
        }

        log.info("Clean up complete.");
    }
}
