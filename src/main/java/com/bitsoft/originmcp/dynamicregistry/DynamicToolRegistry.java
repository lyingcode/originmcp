package com.bitsoft.originmcp.dynamicregistry;

import com.bitsoft.originmcp.mapper.McpToolDefinitionMapper;
import com.bitsoft.originmcp.mapper.McpToolParameterMapper;
import com.bitsoft.originmcp.model.database.McpToolDefinition;
import com.bitsoft.originmcp.model.database.McpToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core registry service that loads MCP tool definitions from the database
 * and dynamically registers them with Spring AI's tool infrastructure.
 *
 * Supports periodic refresh to pick up database changes without restart.
 */
@Service
public class DynamicToolRegistry {
    private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistry.class);

    @Autowired
    private McpToolDefinitionMapper toolDefinitionMapper;

    @Autowired
    private McpToolParameterMapper toolParameterMapper;

    @Autowired
    private ApplicationContext applicationContext;

    // Runtime registry of active tools: toolName -> DynamicToolDef
    private final Map<String, DynamicToolDef> registeredTools = new ConcurrentHashMap<>();

    // Flag to track if initial load has occurred
    private volatile boolean initialized = false;

    /**
     * Loads all enabled tools from database and registers them.
     * Called on startup and during periodic refresh.
     */
    @PostConstruct
    public void initialize() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("Failed to initialize dynamic tool registry - database may not be available: {}", e.getMessage());
            initialized = true;  // Allow app to start even without DB
        }
    }

    /**
     * Refreshes the tool registry from database.
     * Compares with existing registrations to detect additions, updates, and deletions.
     */
    public synchronized void refresh() {
        log.info("Starting tool registry refresh...");

        try {
            // Check if mappers are available
            if (toolDefinitionMapper == null || toolParameterMapper == null) {
                log.warn("Tool mappers not available - skipping refresh");
                return;
            }

            // Load all enabled tool definitions from DB
            List<McpToolDefinition> dbTools = toolDefinitionMapper.findAllEnabled();

            // Build a map of tool definitions keyed by tool name
            Map<String, DynamicToolDef> newRegistry = new ConcurrentHashMap<>();

            for (McpToolDefinition dbTool : dbTools) {
                try {
                    DynamicToolDef toolDef = buildToolDefinition(dbTool);
                    if (toolDef != null) {
                        newRegistry.put(dbTool.getToolName(), toolDef);
                    }
                } catch (Exception e) {
                    log.error("Failed to build tool definition for '{}': {}",
                        dbTool.getToolName(), e.getMessage(), e);
                }
            }

            // Detect changes
            Set<String> added = new HashSet<>(newRegistry.keySet());
            added.removeAll(registeredTools.keySet());

            Set<String> removed = new HashSet<>(registeredTools.keySet());
            removed.removeAll(newRegistry.keySet());

            Set<String> existing = new HashSet<>(newRegistry.keySet());
            existing.retainAll(registeredTools.keySet());

            // Apply changes
            registeredTools.clear();
            registeredTools.putAll(newRegistry);

            // Log summary
            log.info("Tool registry refresh complete: {} total, {} added, {} removed, {} updated",
                registeredTools.size(), added.size(), removed.size(), existing.size());

            if (!added.isEmpty()) {
                log.info("Added tools: {}", added);
            }
            if (!removed.isEmpty()) {
                log.info("Removed tools: {}", removed);
            }

            initialized = true;

        } catch (Exception e) {
            log.error("Failed to refresh tool registry: {}", e.getMessage(), e);
            if (!initialized) {
                // On initial load failure, throw to prevent startup
                throw new RuntimeException("Failed to initialize tool registry", e);
            }
        }
    }

    /**
     * Builds a DynamicToolDef from a database record.
     */
    private DynamicToolDef buildToolDefinition(McpToolDefinition dbTool) {
        String beanName = dbTool.getServiceBeanName();
        Object serviceBean = applicationContext.getBean(beanName);

        // Load parameters for this tool
        List<McpToolParameter> dbParams = toolParameterMapper.findByToolId(dbTool.getId());

        // Convert to ParameterDefinition
        List<DynamicToolDef.ParameterDefinition> parameters = dbParams.stream()
            .map(p -> new DynamicToolDef.ParameterDefinition(
                p.getParameterName(),
                p.getParameterType(),
                p.getDescription(),
                Boolean.TRUE.equals(p.getRequired()),
                p.getDefaultValue()
            ))
            .collect(Collectors.toList());

        // Find the method on the service bean
        Method method = findMethod(serviceBean, dbTool.getMethodName(), parameters);
        if (method == null) {
            log.warn("Method '{}' not found on bean '{}' or parameter count mismatch",
                dbTool.getMethodName(), beanName);
            return null;
        }

        log.debug("Built tool definition: {} -> {}.{}() with {} parameters",
            dbTool.getToolName(), beanName, dbTool.getMethodName(), parameters.size());

        return new DynamicToolDef(
            dbTool.getToolName(),
            dbTool.getDescription(),
            serviceBean,
            method,
            parameters
        );
    }

    /**
     * Finds a method on a service bean matching the given name and parameter count.
     */
    private Method findMethod(Object serviceBean, String methodName,
                             List<DynamicToolDef.ParameterDefinition> parameters) {
        Class<?> beanClass = serviceBean.getClass();
        int paramCount = parameters.size();

        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName) &&
                method.getParameterCount() == paramCount) {
                return method;
            }
        }

        // Fallback: find by name only if exact count match fails
        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns all currently registered tools.
     */
    public Map<String, DynamicToolDef> getRegisteredTools() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(registeredTools));
    }

    /**
     * Returns a specific tool by name, or null if not found.
     */
    public DynamicToolDef getTool(String toolName) {
        return registeredTools.get(toolName);
    }

    /**
     * Returns true if the registry has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the count of registered tools.
     */
    public int getToolCount() {
        return registeredTools.size();
    }
}
