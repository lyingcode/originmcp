package com.bitsoft.originmcp.dynamicregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that periodically refreshes the tool registry
 * to pick up any changes made in the database.
 *
 * Refresh interval is configurable via: tool.registry.refresh-interval
 * Default is 60 seconds (60000 ms).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "tool.registry.enabled", havingValue = "true", matchIfMissing = true)
public class ToolRefreshScheduler {
    private static final Logger log = LoggerFactory.getLogger(ToolRefreshScheduler.class);

    @Autowired
    private DynamicToolRegistry registry;

    /**
     * Scheduled refresh method - runs every refreshInterval milliseconds.
     */
    @Scheduled(fixedDelayString = "${tool.registry.refresh-interval:60000}")
    public void refreshTools() {
        log.debug("Scheduled tool registry refresh triggered");

        try {
            // Refresh the registry from database
            registry.refresh();

            log.info("Scheduled refresh complete. Total tools: {}", registry.getToolCount());

        } catch (Exception e) {
            log.error("Scheduled tool refresh failed: {}", e.getMessage(), e);
            // Don't rethrow - we want the scheduler to continue running
        }
    }

    /**
     * Manually triggers a refresh. Can be called via JMX or REST endpoint.
     */
    public void triggerRefresh() {
        log.info("Manual tool refresh triggered");
        refreshTools();
    }
}
