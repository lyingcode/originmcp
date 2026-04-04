package com.bitsoft.originmcp.dynamicregistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolInvokerTest {

    private ToolInvoker toolInvoker;
    private TestService testService;

    @BeforeEach
    void setUp() {
        toolInvoker = new ToolInvoker();
        testService = new TestService();
    }

    @Test
    void testInvoke_Success() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of(
            new DynamicToolDef.ParameterDefinition("name", "java.lang.String", "", true, null)
        );

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("name", "World");

        // Act
        Object result = toolInvoker.invoke(testService, "greet", arguments, params);

        // Assert
        assertEquals("Hello, World!", result);
    }

    @Test
    void testInvoke_WithDefaultValue() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of(
            new DynamicToolDef.ParameterDefinition("name", "java.lang.String", "", false, "Default")
        );

        Map<String, Object> arguments = new HashMap<>();

        // Act
        Object result = toolInvoker.invoke(testService, "greet", arguments, params);

        // Assert
        assertEquals("Hello, Default!", result);
    }

    @Test
    void testInvoke_MissingRequiredParameter() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of(
            new DynamicToolDef.ParameterDefinition("name", "java.lang.String", "", true, null)
        );

        Map<String, Object> arguments = new HashMap<>();

        // Act & Assert
        assertThrows(ToolInvoker.ToolInvocationException.class, () -> {
            toolInvoker.invoke(testService, "greet", arguments, params);
        });
    }

    @Test
    void testInvoke_MethodNotFound() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of();

        Map<String, Object> arguments = new HashMap<>();

        // Act & Assert
        assertThrows(ToolInvoker.ToolInvocationException.class, () -> {
            toolInvoker.invoke(testService, "nonExistentMethod", arguments, params);
        });
    }

    @Test
    void testInvoke_TypeConversion() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of(
            new DynamicToolDef.ParameterDefinition("count", "java.lang.Integer", "", true, null)
        );

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", "42");  // String instead of Integer

        // Act
        Object result = toolInvoker.invoke(testService, "repeat", arguments, params);

        // Assert
        assertEquals(42, result);
    }

    @Test
    void testInvoke_MultipleParameters() throws NoSuchMethodException {
        // Arrange
        List<DynamicToolDef.ParameterDefinition> params = List.of(
            new DynamicToolDef.ParameterDefinition("greeting", "java.lang.String", "", true, null),
            new DynamicToolDef.ParameterDefinition("name", "java.lang.String", "", true, null)
        );

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("greeting", "Hi");
        arguments.put("name", "Alice");

        // Act
        Object result = toolInvoker.invoke(testService, "customGreet", arguments, params);

        // Assert
        assertEquals("Hi, Alice!", result);
    }

    @Test
    void testToolInvocationException() {
        // Test exception can be created with message
        ToolInvoker.ToolInvocationException ex = new ToolInvoker.ToolInvocationException("Test error");
        assertEquals("Test error", ex.getMessage());

        // Test exception can be created with cause
        Throwable cause = new RuntimeException("Cause");
        ToolInvoker.ToolInvocationException exWithCause = new ToolInvoker.ToolInvocationException("With cause", cause);
        assertEquals("With cause", exWithCause.getMessage());
        assertEquals(cause, exWithCause.getCause());
    }

    // Test service class for ToolInvoker tests
    static class TestService {
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        public int repeat(String count) {
            return Integer.parseInt(count);
        }

        public String customGreet(String greeting, String name) {
            return greeting + ", " + name + "!";
        }
    }
}
