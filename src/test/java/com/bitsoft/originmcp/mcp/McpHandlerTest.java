package com.bitsoft.originmcp.mcp;

import com.bitsoft.originmcp.dynamicregistry.DynamicToolDef;
import com.bitsoft.originmcp.dynamicregistry.DynamicToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpHandlerTest {

    @Mock
    private DynamicToolRegistry toolRegistry;

    private ObjectMapper objectMapper;
    private McpHandler mcpHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mcpHandler = new McpHandler(objectMapper, toolRegistry);
    }

    @Test
    void testHandleOptions_ReturnsCorsHeaders() {
        ServerRequest request = mock(ServerRequest.class);

        Mono<ServerResponse> responseMono = mcpHandler.handleOptions(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
                assertEquals("*", response.headers().getFirst("Access-Control-Allow-Origin"));
                assertEquals("GET, POST, OPTIONS", response.headers().getFirst("Access-Control-Allow-Methods"));
                assertEquals("Content-Type, X-API-Key", response.headers().getFirst("Access-Control-Allow-Headers"));
            })
            .verifyComplete();
    }

    @Test
    void testHandleJsonRpc_ParseError_InvalidJson() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("invalid json{"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
            })
            .verifyComplete();
    }

    @Test
    void testHandleJsonRpc_EmptyBody() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just(""));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
            })
            .verifyComplete();
    }

    @Test
    void testHandleJsonRpc_InvalidRequest_MissingJsonrpcVersion() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("{\"method\":\"test\",\"id\":1}"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
            })
            .verifyComplete();
    }

    @Test
    void testHandleJsonRpc_InvalidRequest_EmptyMethod() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("{\"jsonrpc\":\"2.0\",\"method\":\"\",\"id\":1}"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
            })
            .verifyComplete();
    }

    @Test
    void testHandleJsonRpc_ToolsList_NoTools() {
        when(toolRegistry.getRegisteredTools()).thenReturn(Map.of());

        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":1}"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
            })
            .verifyComplete();

        verify(toolRegistry).getRegisteredTools();
    }

    @Test
    void testHandleJsonRpc_MethodNotFound() {
        when(toolRegistry.getTool("unknownTool")).thenReturn(null);

        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("{\"jsonrpc\":\"2.0\",\"method\":\"unknownTool\",\"id\":1}"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
            })
            .verifyComplete();

        verify(toolRegistry).getTool("unknownTool");
    }

    @Test
    void testHandleJsonRpc_Notification_NoId() {
        when(toolRegistry.getRegisteredTools()).thenReturn(Map.of());

        ServerRequest request = mock(ServerRequest.class);
        when(request.bodyToMono(String.class)).thenReturn(Mono.just("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\"}"));

        Mono<ServerResponse> responseMono = mcpHandler.handleJsonRpc(request);

        StepVerifier.create(responseMono)
            .assertNext(response -> {
                assertEquals(HttpStatus.OK, response.statusCode());
            })
            .verifyComplete();
    }

    @Test
    void testMapJavaTypeToJsonSchemaType() {
        assertEquals("string", mcpHandler.mapJavaTypeToJsonSchemaType("java.lang.String"));
        assertEquals("string", mcpHandler.mapJavaTypeToJsonSchemaType("String"));
        assertEquals("string", mcpHandler.mapJavaTypeToJsonSchemaType("java.util.Date"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("java.lang.Integer"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("int"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("java.lang.Long"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("long"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("java.lang.Double"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("double"));
        assertEquals("number", mcpHandler.mapJavaTypeToJsonSchemaType("java.math.BigDecimal"));
        assertEquals("boolean", mcpHandler.mapJavaTypeToJsonSchemaType("java.lang.Boolean"));
        assertEquals("boolean", mcpHandler.mapJavaTypeToJsonSchemaType("boolean"));
        assertEquals("string", mcpHandler.mapJavaTypeToJsonSchemaType(null));
    }

    @Test
    void testEscapeJson() {
        assertEquals("", mcpHandler.escapeJson(null));
        assertEquals("hello", mcpHandler.escapeJson("hello"));
        assertEquals("hello\\\\world", mcpHandler.escapeJson("hello\\world"));
        assertEquals("hello\\\"world", mcpHandler.escapeJson("hello\"world"));
        assertEquals("line1\\nline2", mcpHandler.escapeJson("line1\nline2"));
        assertEquals("line1\\rline2", mcpHandler.escapeJson("line1\rline2"));
        assertEquals("col1\\tcol2", mcpHandler.escapeJson("col1\tcol2"));
    }

    @Test
    void testBuildSuccessResponse() {
        String response = mcpHandler.buildSuccessResponse(1, Map.of("result", "test"));
        assertTrue(response.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(response.contains("\"id\":1"));
        assertTrue(response.contains("\"result\""));
    }

    @Test
    void testBuildErrorResponse() {
        String response = mcpHandler.buildErrorResponse(1, -32600, "Invalid Request");
        assertTrue(response.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(response.contains("\"id\":1"));
        assertTrue(response.contains("\"error\""));
        assertTrue(response.contains("\"code\":-32600"));
        assertTrue(response.contains("\"message\":\"Invalid Request\""));
    }

    @Test
    void testBuildErrorResponse_NullId() {
        String response = mcpHandler.buildErrorResponse(null, -32600, "Error message");
        // When id is null, it becomes the string "null" in JSON
        assertTrue(response.contains("\"id\":\"null\"") || response.contains("\"id\":null"));
    }
}
