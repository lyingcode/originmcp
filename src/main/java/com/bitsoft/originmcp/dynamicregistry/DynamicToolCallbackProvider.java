package com.bitsoft.originmcp.dynamicregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        // Create tool definition - inputSchema needs to be a JSON string
        String inputSchemaJson;
        try {
            inputSchemaJson = objectMapper.writeValueAsString(toolDef.getInputSchema());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize input schema for tool '{}': {}", toolDef.getName(), e.getMessage());
            inputSchemaJson = "{\"type\":\"object\"}";
        }

        ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name(toolDef.getName())
            .description(toolDef.getDescription())
            .inputSchema(inputSchemaJson)
            .build();

        final DynamicToolDef finalToolDef = toolDef;
        final ToolInvoker finalToolInvoker = toolInvoker;

        // Create a custom callback that uses ToolInvoker
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String input) {
                try {
                    // Parse the JSON input into a Map
                    @SuppressWarnings("unchecked")
                    Map<String, Object> argMap = objectMapper.readValue(input, Map.class);

                    // Invoke the tool
                    Object result = finalToolInvoker.invoke(
                        finalToolDef.getServiceBean(),
                        finalToolDef.getMethod().getName(),
                        argMap,
                        finalToolDef.getParameters()
                    );

                    // Return the result as JSON string
                    return objectMapper.writeValueAsString(result);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse input JSON for tool '{}': {}", finalToolDef.getName(), e.getMessage());
                    return "{\"error\": \"Invalid JSON input: " + e.getMessage() + "\"}";
                } catch (Exception e) {
                    log.error("Tool invocation failed for '{}': {}", finalToolDef.getName(), e.getMessage());
                    return "{\"error\": \"" + e.getMessage() + "\"}";
                }
            }
        };
    }
}
