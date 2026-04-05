package com.bitsoft.originmcp.mcp;

import com.bitsoft.originmcp.dynamicregistry.DynamicToolDef;
import com.bitsoft.originmcp.mapper.McpToolDefinitionMapper;
import com.bitsoft.originmcp.mapper.McpToolParameterMapper;
import com.bitsoft.originmcp.model.database.McpToolDefinition;
import com.bitsoft.originmcp.model.database.McpToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server 配置 - 简化版。
 * 利用 Spring AI MCP 自动配置，仅提供工具注册。
 */
@Configuration
public class McpServerConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    @Autowired
    private McpToolDefinitionMapper toolDefinitionMapper;

    @Autowired
    private McpToolParameterMapper toolParameterMapper;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Value("${tool.registry.refresh-interval:60000}")
    private long refreshInterval;

    // 运行时工具注册表
    private final Map<String, DynamicToolDef> registeredTools = new ConcurrentHashMap<>();

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        refreshTools();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        refreshTools();
    }

    @Scheduled(fixedDelayString = "${tool.registry.refresh-interval:60000}")
    public void scheduledRefresh() {
        refreshTools();
    }

    // ==================== 工具加载 ====================

    public synchronized void refreshTools() {
        try {
            if (toolDefinitionMapper == null || toolParameterMapper == null) {
                log.warn("Tool mappers not available");
                return;
            }

            List<McpToolDefinition> dbTools = toolDefinitionMapper.findAllEnabled();
            Map<String, DynamicToolDef> newRegistry = new ConcurrentHashMap<>();

            for (McpToolDefinition dbTool : dbTools) {
                DynamicToolDef toolDef = buildToolDefinition(dbTool);
                if (toolDef != null) {
                    newRegistry.put(dbTool.getToolName(), toolDef);
                }
            }

            registeredTools.clear();
            registeredTools.putAll(newRegistry);
            log.info("Tool registry refreshed: {} tools", registeredTools.size());

        } catch (Exception e) {
            log.error("Failed to refresh tools: {}", e.getMessage());
        }
    }

    private DynamicToolDef buildToolDefinition(McpToolDefinition dbTool) {
        String beanName = dbTool.getServiceBeanName();
        Object serviceBean = applicationContext.getBean(beanName);

        List<McpToolParameter> dbParams = toolParameterMapper.findByToolId(dbTool.getId());
        List<DynamicToolDef.ParameterDefinition> parameters = dbParams.stream()
            .map(p -> new DynamicToolDef.ParameterDefinition(
                p.getParameterName(), p.getParameterType(),
                p.getDescription(), Boolean.TRUE.equals(p.getRequired()), p.getDefaultValue()))
            .toList();

        Method method = findMethod(serviceBean, dbTool.getMethodName(), parameters);
        if (method == null) {
            log.warn("Method not found: {} on {}", dbTool.getMethodName(), beanName);
            return null;
        }

        return new DynamicToolDef(dbTool.getToolName(), dbTool.getDescription(),
            serviceBean, method, parameters);
    }

    private Method findMethod(Object bean, String methodName,
                              List<DynamicToolDef.ParameterDefinition> parameters) {
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == parameters.size()) {
                return m;
            }
        }
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() > 0) {
                return m;
            }
        }
        return null;
    }

    // ==================== 工具调用 ====================

    public Object invokeTool(DynamicToolDef tool, Map<String, Object> arguments) throws Exception {
        Method method = tool.getMethod();
        Object[] args = buildArguments(method, arguments, tool.getParameters());
        method.setAccessible(true);
        return method.invoke(tool.getServiceBean(), args);
    }

    private Object[] buildArguments(Method method, Map<String, Object> arguments,
                                   List<DynamicToolDef.ParameterDefinition> paramDefs) {
        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < paramDefs.size() && i < args.length; i++) {
            DynamicToolDef.ParameterDefinition paramDef = paramDefs.get(i);
            Object value = arguments.get(paramDef.getName());
            if (value == null && paramDef.getDefaultValue() != null) {
                value = convert(paramDef.getDefaultValue(), method.getParameterTypes()[i]);
            }
            args[i] = convert(value, method.getParameterTypes()[i]);
        }
        return args;
    }

    private Object convert(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) return value;
        if (value instanceof String s) {
            if (target == int.class || target == Integer.class) return Integer.parseInt(s);
            if (target == long.class || target == Long.class) return Long.parseLong(s);
            if (target == double.class || target == Double.class) return Double.parseDouble(s);
            if (target == boolean.class || target == Boolean.class) return Boolean.parseBoolean(s);
        }
        return value;
    }

    // ==================== Spring AI 集成 ====================

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            org.springframework.beans.factory.ObjectFactory<com.fasterxml.jackson.databind.ObjectMapper> objectMapperFactory) {
        log.info("Creating ToolCallbackProvider with {} tools", registeredTools.size());
        // 返回一个每次调用 getToolCallbacks() 时动态获取工具的 Provider
        return new ToolCallbackProvider() {
            private volatile Map<String, DynamicToolDef> lastSnapshot = Map.of();

            @Override
            public ToolCallback[] getToolCallbacks() {
                Map<String, DynamicToolDef> current = Map.copyOf(registeredTools);
                if (!current.equals(lastSnapshot)) {
                    lastSnapshot = current;
                    log.info("Providing {} tool callbacks", current.size());
                }
                return current.values().stream()
                    .map(tool -> createCallback(tool, objectMapperFactory.getObject()))
                    .toArray(ToolCallback[]::new);
            }
        };
    }

    private ToolCallback createCallback(DynamicToolDef tool, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        ToolDefinition def = DefaultToolDefinition.builder()
            .name(tool.getName())
            .description(tool.getDescription())
            .inputSchema(toJson(objectMapper, tool.getInputSchema()))
            .build();

        DynamicToolDef t = tool;
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() { return def; }

            @Override
            public String call(String input) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = objectMapper.readValue(input, Map.class);
                    Object result = invokeTool(t, args);
                    return objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    return "{\"error\": \"" + e.getMessage() + "\"}";
                }
            }
        };
    }

    private String toJson(com.fasterxml.jackson.databind.ObjectMapper objectMapper, Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"type\":\"object\"}";
        }
    }

    // ==================== 访问器 ====================

    public Map<String, DynamicToolDef> getRegisteredTools() {
        return Map.copyOf(registeredTools);
    }

    public int getToolCount() {
        return registeredTools.size();
    }
}
