package com.bitsoft.originmcp.controller;

import com.bitsoft.originmcp.dynamicregistry.DynamicToolDef;
import com.bitsoft.originmcp.dynamicregistry.DynamicToolRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * HTTP endpoint controller for MCP JSON-RPC 2.0 requests.
 * Provides stateless HTTP transport for MCP tool invocations.
 */
@RestController
@RequestMapping("/origin/mcp")
public class McpHttpController {

    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);

    private static final String JSONRPC_VERSION = "2.0";

    // JSON-RPC error codes
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int TOOL_ERROR = -32000;

    @Autowired
    private DynamicToolRegistry toolRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Handle POST requests to /origin/mcp with JSON-RPC 2.0 request body.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleJsonRpc(@RequestBody String rawBody) {
        // 1. Parse JSON-RPC request
        JsonRpcRequest request;
        try {
            request = parseRequest(rawBody);
        } catch (JsonRpcParseException e) {
            log.warn("JSON-RPC parse error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(null, PARSE_ERROR, e.getMessage()));
        }

        // 2. Validate request
        if (!isValidRequest(request)) {
            return ResponseEntity.badRequest().body(buildErrorResponse(request.id, INVALID_REQUEST, "Invalid Request"));
        }

        // 3. Check if it's a notification (no id) - still process but don't respond
        if (request.id == null) {
            log.debug("Received JSON-RPC notification for method: {}", request.method);
            processNotification(request);
            return ResponseEntity.ok().build();
        }

        // 4. Find and invoke tool
        try {
            Object result = invokeTool(request.method, request.params);
            return ResponseEntity.ok(buildSuccessResponse(request.id, result));
        } catch (JsonRpcException e) {
            log.warn("JSON-RPC error for method '{}': {}", request.method, e.getMessage());
            return ResponseEntity.ok(buildErrorResponse(request.id, e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Tool invocation failed for method '{}': {}", request.method, e.getMessage(), e);
            return ResponseEntity.ok(buildErrorResponse(request.id, TOOL_ERROR, e.getMessage()));
        }
    }

    /**
     * Handle OPTIONS requests for CORS preflight.
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type, X-API-Key")
            .build();
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
     * Process a notification (request without id).
     */
    private void processNotification(JsonRpcRequest request) {
        try {
            invokeTool(request.method, request.params);
        } catch (Exception e) {
            log.warn("Notification processing failed for method '{}': {}", request.method, e.getMessage());
        }
    }

    /**
     * Invoke a tool by method name with parameters.
     */
    private Object invokeTool(String methodName, Map<String, Object> params) throws JsonRpcException {
        // Handle tools/list method - return list of all registered tools
        if ("tools/list".equals(methodName) || "listTools".equals(methodName)) {
            return listTools();
        }

        DynamicToolDef tool = toolRegistry.getTool(methodName);
        if (tool == null) {
            throw new JsonRpcException(METHOD_NOT_FOUND, "Method not found: " + methodName);
        }

        try {
            // Convert params map to method arguments
            Object[] args = buildMethodArgs(tool, params);
            Object result = tool.getMethod().invoke(tool.getServiceBean(), args);
            return result;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                throw new JsonRpcException(TOOL_ERROR, cause.getMessage());
            }
            throw new JsonRpcException(TOOL_ERROR, "Tool execution failed");
        } catch (Exception e) {
            throw new JsonRpcException(TOOL_ERROR, "Tool invocation failed: " + e.getMessage());
        }
    }

    /**
     * List all registered tools in MCP protocol format.
     */
    private Map<String, Object> listTools() {
        Map<String, DynamicToolDef> tools = toolRegistry.getRegisteredTools();
        java.util.List<Map<String, Object>> toolList = new java.util.ArrayList<>();

        for (DynamicToolDef tool : tools.values()) {
            Map<String, Object> toolInfo = new java.util.LinkedHashMap<>();
            toolInfo.put("name", tool.getName());
            toolInfo.put("description", tool.getDescription());

            // Build input schema for the tool
            Map<String, Object> inputSchema = new java.util.LinkedHashMap<>();
            inputSchema.put("type", "object");
            Map<String, Object> properties = new java.util.LinkedHashMap<>();
            java.util.List<String> requiredParams = new java.util.ArrayList<>();

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

            toolInfo.put("inputSchema", inputSchema);
            toolList.add(toolInfo);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("tools", toolList);
        return result;
    }

    /**
     * Map Java type to JSON Schema type.
     */
    private String mapJavaTypeToJsonSchemaType(String javaType) {
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

        java.util.List<DynamicToolDef.ParameterDefinition> paramDefs = tool.getParameters();
        Object[] args = new Object[paramDefs.size()];

        for (int i = 0; i < paramDefs.size(); i++) {
            DynamicToolDef.ParameterDefinition paramDef = paramDefs.get(i);
            Object value = params.get(paramDef.getName());
            // Use default value if not provided
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
    private String buildSuccessResponse(Object id, Object result) {
        try {
            JsonRpcResponse response = new JsonRpcResponse();
            response.jsonrpc = JSONRPC_VERSION;
            response.id = id;
            response.result = result;
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize success response: {}", e.getMessage());
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Serialization error\"},\"id\":null}";
        }
    }

    /**
     * Build JSON-RPC error response.
     */
    private String buildErrorResponse(Object id, int code, String message) {
        try {
            JsonRpcResponse response = new JsonRpcResponse();
            response.jsonrpc = JSONRPC_VERSION;
            response.id = id;
            response.error = new JsonRpcError(code, message);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize error response: {}", e.getMessage());
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\"},\"id\":" + (id != null ? "\"" + id + "\"" : "null") + "}";
        }
    }

    /**
     * Simple JSON escaping for error messages.
     */
    private String escapeJson(String s) {
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
     * JSON-RPC response object.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class JsonRpcResponse {
        @JsonProperty("jsonrpc")
        String jsonrpc;

        @JsonProperty("id")
        Object id;

        @JsonProperty("result")
        Object result;

        @JsonProperty("error")
        JsonRpcError error;
    }

    /**
     * JSON-RPC error object.
     */
    private static class JsonRpcError {
        @JsonProperty("code")
        int code;

        @JsonProperty("message")
        String message;

        JsonRpcError() {}

        JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
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
