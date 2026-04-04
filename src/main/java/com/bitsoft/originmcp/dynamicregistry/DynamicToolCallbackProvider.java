package com.bitsoft.originmcp.dynamicregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinitionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Tool Callback Provider that loads tools from database.
 * Creates custom ToolCallback instances without @Tool annotation.
 */
@Component
public class DynamicToolCallbackProvider implements ToolCallbackProvider {
    private static final Logger log = LoggerFactory.getLogger(DynamicToolCallbackProvider.class);

    @Autowired
    private DynamicToolRegistry registry;

    @Autowired
    private ToolInvoker toolInvoker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("Initializing DynamicToolCallbackProvider");
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        try {
            // Get all registered tools from the registry
            Map<String, DynamicToolDef> tools = registry.getRegisteredTools();

            if (tools.isEmpty()) {
                log.warn("No dynamic tools registered");
                return new ToolCallback[0];
            }

            // Create a ToolCallback for each tool
            List<ToolCallback> callbacks = new ArrayList<>();
            for (DynamicToolDef toolDef : tools.values()) {
                try {
                    ToolCallback callback = createToolCallback(toolDef);
                    callbacks.add(callback);
                    log.debug("Created callback for tool: {}", toolDef.getName());
                } catch (Exception e) {
                    log.error("Failed to create callback for tool '{}': {}", toolDef.getName(), e.getMessage());
                }
            }

            log.info("Created {} tool callbacks for {} tools", callbacks.size(), tools.size());
            return callbacks.toArray(new ToolCallback[0]);

        } catch (Exception e) {
            log.error("Failed to create tool callbacks: {}", e.getMessage(), e);
            return new ToolCallback[0];
        }
    }

    private ToolCallback createToolCallback(DynamicToolDef toolDef) {
        // Create tool definition
        ToolDefinition toolDefinition = ToolDefinitionBuilder.builder()
            .name(toolDef.getName())
            .description(toolDef.getDescription())
            .inputSchema(toolDef.getInputSchema())
            .build();

        // Create a custom callback that uses ToolInvoker
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object call(Object... arguments) {
                try {
                    // Build argument map from the arguments array
                    Map<String, Object> argMap = new java.util.LinkedHashMap<>();
                    var params = toolDef.getParameters();
                    for (int i = 0; i < arguments.length && i < params.size(); i++) {
                        argMap.put(params.get(i).getName(), arguments[i]);
                    }

                    return toolInvoker.invoke(
                        toolDef.getServiceBean(),
                        toolDef.getMethod().getName(),
                        argMap,
                        params
                    );
                } catch (Exception e) {
                    log.error("Tool invocation failed for '{}': {}", toolDef.getName(), e.getMessage());
                    throw new RuntimeException("Tool invocation failed: " + e.getMessage(), e);
                }
            }
        };
    }
}
