package com.bitsoft.originmcp.mcp;

import com.bitsoft.originmcp.dynamicregistry.DynamicToolDef;
import com.bitsoft.originmcp.dynamicregistry.DynamicToolRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reactive handler for MCP JSON-RPC 2.0 requests.
 * Decoupled from Spring MVC Controller, purely functional.
 */
@Component
public class McpHandler {

    private static final Logger log = LoggerFactory.getLogger(McpHandler.class);

    private static final String JSONRPC_VERSION = "2.0";
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int TOOL_ERROR = -32000;

    private DynamicToolRegistry toolRegistry;

    private ObjectMapper objectMapper;

    private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

    @Autowired
    public McpHandler(ObjectMapper objectMapper, DynamicToolRegistry toolRegistry) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Handle POST requests with JSON-RPC 2.0 request body.
     */
    public Mono<ServerResponse> handleJsonRpc(ServerRequest request) {
        return request.bodyToMono(String.class)
            .flatMap(this::processRequest)
            .onErrorResume(e -> {
                log.error("Request processing error: {}", e.getMessage());
                return ServerResponse.badRequest()
                    .bodyValue(buildErrorResponse(null, PARSE_ERROR, e.getMessage()));
            });
    }

    /**
     * Handle CORS preflight OPTIONS requests.
     */
    public Mono<ServerResponse> handleOptions(ServerRequest request) {
        return ServerResponse.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type, X-API-Key")
            .build();
    }

    /**
     * Handle SSE endpoint for Roo Code / clients that require SSE transport.
     * Returns a stream of JSON-RPC responses via Server-Sent Events.
     */
    public Mono<ServerResponse> handleSse(ServerRequest request) {
        // Return endpoint info in a format that HTTP clients expect
        // Line-based format: "url: <actual-post-url>"
        String sseData = "data: {\"protocol\":\"http\",\"postUrl\":\"/origin/mcp\",\"message\":\"MCP server ready for Roo Code\"}\n\n";

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Access-Control-Allow-Origin", "*")
            .header("Cache-Control", "no-cache")
            .body(BodyInserters.fromDataBuffers(Flux.just(
                BUFFER_FACTORY.wrap(sseData.getBytes(StandardCharsets.UTF_8))
            )));
    }

    /**
     * Process JSON-RPC request and return response.
     */
    private Mono<ServerResponse> processRequest(String rawBody) {
        try {
            JsonRpcRequest request = parseRequest(rawBody);

            if (!isValidRequest(request)) {
                return ServerResponse.ok()
                    .bodyValue(buildErrorResponse(request != null ? request.id : null,
                        INVALID_REQUEST, "Invalid Request"));
            }

            // Handle notification (no id) - still process but return empty response
            if (request.id == null) {
                log.debug("Received JSON-RPC notification for method: {}", request.method);
                invokeTool(request.method, request.params).subscribe();
                return ServerResponse.ok().build();
            }

            // Invoke tool and return result
            return invokeTool(request.method, request.params)
                .flatMap(result -> ServerResponse.ok()
                    .bodyValue(buildSuccessResponse(request.id, result)))
                .onErrorResume(e -> {
                    JsonRpcException jre = e.getCause() instanceof JsonRpcException
                        ? (JsonRpcException) e.getCause()
                        : new JsonRpcException(TOOL_ERROR, e.getMessage());
                    return ServerResponse.ok()
                        .bodyValue(buildErrorResponse(request.id, jre.getCode(), jre.getMessage()));
                });

        } catch (JsonRpcParseException e) {
            return ServerResponse.badRequest()
                .bodyValue(buildErrorResponse(null, PARSE_ERROR, e.getMessage()));
        }
    }

    /**
     * Parse JSON-RPC request from raw body.
     */
    private JsonRpcRequest parseRequest(String rawBody) throws JsonRpcParseException {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            throw new JsonRpcParseException("Request body is empty");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawBody, Map.class);
            return new JsonRpcRequest(map);
        } catch (Exception e) {
            throw new JsonRpcParseException("Parse error");
        }
    }

    /**
     * Validate JSON-RPC request structure.
     */
    private boolean isValidRequest(JsonRpcRequest request) {
        if (request == null) return false;
        if (!JSONRPC_VERSION.equals(request.jsonrpc)) return false;
        if (request.method == null || request.method.trim().isEmpty()) return false;
        return true;
    }

    /**
     * Invoke a tool by method name with parameters.
     */
    private Mono<Map<String, Object>> invokeTool(String methodName, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            // Handle tools/list method
            if ("tools/list".equals(methodName) || "listTools".equals(methodName)) {
                return listTools();
            }

            DynamicToolDef tool = toolRegistry.getTool(methodName);
            if (tool == null) {
                throw new JsonRpcException(METHOD_NOT_FOUND, "Method not found: " + methodName);
            }

            try {
                Object[] args = buildMethodArgs(tool, params);
                Object result = tool.getMethod().invoke(tool.getServiceBean(), args);
                return Map.of("result", result);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    throw new RuntimeException(new JsonRpcException(TOOL_ERROR, cause.getMessage()));
                }
                throw new RuntimeException(new JsonRpcException(TOOL_ERROR, "Tool execution failed"));
            } catch (Exception e) {
                throw new RuntimeException(new JsonRpcException(TOOL_ERROR, "Tool invocation failed: " + e.getMessage()));
            }
        });
    }

    /**
     * List all registered tools in MCP protocol format.
     */
    private Map<String, Object> listTools() {
        Map<String, DynamicToolDef> tools = toolRegistry.getRegisteredTools();
        List<Map<String, Object>> toolList = tools.values().stream()
            .map(this::buildToolInfo)
            .collect(Collectors.toList());

        return Map.of("tools", toolList);
    }

    /**
     * Build tool info map for JSON-RPC response.
     */
    private Map<String, Object> buildToolInfo(DynamicToolDef tool) {
        Map<String, Object> inputSchema = new java.util.LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        List<String> requiredParams = new java.util.ArrayList<>();

        for (DynamicToolDef.ParameterDefinition param : tool.getParameters()) {
            Map<String, Object> paramSchema = new java.util.LinkedHashMap<>();
            paramSchema.put("type", mapJavaTypeToJsonSchemaType(param.getParameterType()));
            paramSchema.put("description", param.getDescription());
            properties.put(param.getName(), paramSchema);
            if (param.isRequired()) {
                requiredParams.add(param.getName());
            }
        }

        inputSchema.put("properties", properties);
        if (!requiredParams.isEmpty()) {
            inputSchema.put("required", requiredParams);
        }

        return Map.of(
            "name", tool.getName(),
            "description", tool.getDescription(),
            "inputSchema", inputSchema
        );
    }

    /**
     * Map Java type to JSON Schema type.
     */
    String mapJavaTypeToJsonSchemaType(String javaType) {
        if (javaType == null) return "string";
        String type = javaType.toLowerCase();
        if (type.contains("string")) return "string";
        if (type.contains("int") || type.contains("long") || type.contains("short") ||
            type.contains("byte") || type.contains("integer")) return "number";
        if (type.contains("float") || type.contains("double") || type.contains("decimal")) return "number";
        if (type.contains("boolean")) return "boolean";
        return "string";
    }

    /**
     * Build method arguments from params map based on tool definition.
     */
    private Object[] buildMethodArgs(DynamicToolDef tool, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new Object[0];
        }

        List<DynamicToolDef.ParameterDefinition> paramDefs = tool.getParameters();
        Object[] args = new Object[paramDefs.size()];

        for (int i = 0; i < paramDefs.size(); i++) {
            DynamicToolDef.ParameterDefinition paramDef = paramDefs.get(i);
            Object value = params.get(paramDef.getName());
            if (value == null && paramDef.getDefaultValue() != null) {
                value = convertDefaultValue(paramDef);
            }
            args[i] = value;
        }

        return args;
    }

    /**
     * Convert default value string to appropriate type.
     */
    private Object convertDefaultValue(DynamicToolDef.ParameterDefinition paramDef) {
        String defaultValue = paramDef.getDefaultValue();
        String paramType = paramDef.getParameterType();

        if (defaultValue == null) return null;

        try {
            if ("java.lang.Integer".equals(paramType) || "int".equals(paramType)) {
                return Integer.parseInt(defaultValue);
            } else if ("java.lang.Long".equals(paramType) || "long".equals(paramType)) {
                return Long.parseLong(defaultValue);
            } else if ("java.lang.Double".equals(paramType) || "double".equals(paramType)) {
                return Double.parseDouble(defaultValue);
            } else if ("java.lang.Boolean".equals(paramType) || "boolean".equals(paramType)) {
                return Boolean.parseBoolean(defaultValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse default value '{}' for parameter '{}'", defaultValue, paramDef.getName());
        }

        return defaultValue;
    }

    /**
     * Build JSON-RPC success response.
     */
    String buildSuccessResponse(Object id, Object result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "jsonrpc", JSONRPC_VERSION,
                "id", id,
                "result", result
            ));
        } catch (Exception e) {
            log.error("Failed to serialize success response: {}", e.getMessage());
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Serialization error\"},\"id\":null}";
        }
    }

    /**
     * Build JSON-RPC error response.
     */
    String buildErrorResponse(Object id, int code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "jsonrpc", JSONRPC_VERSION,
                "id", id != null ? id : "null",
                "error", Map.of("code", code, "message", message)
            ));
        } catch (Exception e) {
            log.error("Failed to serialize error response: {}", e.getMessage());
            return String.format("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":%d,\"message\":\"%s\"},\"id\":%s}",
                code, escapeJson(message), id != null ? "\"" + id + "\"" : "null");
        }
    }

    /**
     * Simple JSON escaping for error messages.
     */
    String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * JSON-RPC request object.
     */
    private static class JsonRpcRequest {
        @JsonProperty("jsonrpc")
        String jsonrpc;

        @JsonProperty("method")
        String method;

        @JsonProperty("params")
        Map<String, Object> params;

        @JsonProperty("id")
        Object id;

        JsonRpcRequest() {}

        JsonRpcRequest(Map<String, Object> map) {
            this.jsonrpc = (String) map.get("jsonrpc");
            this.method = (String) map.get("method");
            this.params = (Map<String, Object>) map.get("params");
            this.id = map.get("id");
        }
    }

    /**
     * JSON-RPC exception with error code.
     */
    private static class JsonRpcException extends Exception {
        private final int code;

        JsonRpcException(int code, String message) {
            super(message);
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }

    /**
     * JSON-RPC parse exception.
     */
    private static class JsonRpcParseException extends Exception {
        JsonRpcParseException(String message) {
            super(message);
        }
    }
}
