package com.bitsoft.originmcp.dynamicregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Tool Callback Provider that loads tools from database.
 * Uses Spring's MethodToolCallbackProvider to wrap service objects.
 */
@Component
public class DynamicToolCallbackProvider implements ToolCallbackProvider {
    private static final Logger log = LoggerFactory.getLogger(DynamicToolCallbackProvider.class);

    @Autowired
    private DynamicToolRegistry registry;

    private ToolCallbackProvider delegateProvider;

    @PostConstruct
    public void init() {
        log.info("Initializing DynamicToolCallbackProvider");
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        try {
            // Get all registered service beans from the registry
            Map<String, DynamicToolDef> tools = registry.getRegisteredTools();

            if (tools.isEmpty()) {
                log.warn("No dynamic tools registered");
                return new ToolCallback[0];
            }

            // Collect unique service beans
            List<Object> serviceBeans = new ArrayList<>();
            for (DynamicToolDef tool : tools.values()) {
                Object bean = tool.getServiceBean();
                if (!serviceBeans.contains(bean)) {
                    serviceBeans.add(bean);
                }
            }

            if (serviceBeans.isEmpty()) {
                return new ToolCallback[0];
            }

            // Use MethodToolCallbackProvider with all service beans
            delegateProvider = MethodToolCallbackProvider.builder()
                .toolObjects(serviceBeans.toArray())
                .build();

            log.info("Created ToolCallbackProvider with {} service beans for {} tools",
                serviceBeans.size(), tools.size());

            return delegateProvider.getToolCallbacks();

        } catch (Exception e) {
            log.error("Failed to create tool callbacks: {}", e.getMessage(), e);
            return new ToolCallback[0];
        }
    }
}
