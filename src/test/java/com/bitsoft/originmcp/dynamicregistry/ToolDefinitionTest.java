package com.bitsoft.originmcp.dynamicregistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ToolDefinitionTest {

    private Method getToStringMethod() throws NoSuchMethodException {
        // Find a method that takes no arguments
        for (Method m : Object.class.getMethods()) {
            if (m.getName().equals("toString") && m.getParameterCount() == 0) {
                return m;
            }
        }
        throw new NoSuchMethodException("toString()");
    }

    @Test
    void testGetInputSchema_WithRequiredParameters() throws NoSuchMethodException {
        // Arrange
        DynamicToolDef.ParameterDefinition param1 = new DynamicToolDef.ParameterDefinition(
            "location", "java.lang.String", "City name", true, null
        );
        DynamicToolDef.ParameterDefinition param2 = new DynamicToolDef.ParameterDefinition(
            "days", "java.lang.Integer", "Number of days", false, "7"
        );

        List<DynamicToolDef.ParameterDefinition> params = List.of(param1, param2);

        DynamicToolDef toolDef = new DynamicToolDef(
            "getWeather",
            "Get weather for a location",
            new Object(),
            getToStringMethod(),
            params
        );

        // Act
        Map<String, Object> schema = toolDef.getInputSchema();

        // Assert
        assertEquals("object", schema.get("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertTrue(properties.containsKey("location"));
        assertTrue(properties.containsKey("days"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.contains("location"));
        assertFalse(required.contains("days"));
    }

    @Test
    void testGetInputSchema_NoRequiredParameters() throws NoSuchMethodException {
        // Arrange
        DynamicToolDef.ParameterDefinition param1 = new DynamicToolDef.ParameterDefinition(
            "query", "java.lang.String", "Search query", false, "default"
        );

        List<DynamicToolDef.ParameterDefinition> params = List.of(param1);

        DynamicToolDef toolDef = new DynamicToolDef(
            "search",
            "Search for something",
            new Object(),
            getToStringMethod(),
            params
        );

        // Act
        Map<String, Object> schema = toolDef.getInputSchema();

        // Assert
        assertEquals("object", schema.get("type"));
        // When no parameters are required, the "required" key should not be present
        assertFalse(schema.containsKey("required"));
    }

    @Test
    void testParameterDefinition_Getters() {
        // Arrange
        DynamicToolDef.ParameterDefinition param = new DynamicToolDef.ParameterDefinition(
            "location",
            "java.lang.String",
            "City name",
            true,
            "beijing"
        );

        // Assert
        assertEquals("location", param.getName());
        assertEquals("java.lang.String", param.getParameterType());
        assertEquals("City name", param.getDescription());
        assertTrue(param.isRequired());
        assertEquals("beijing", param.getDefaultValue());
    }

    @Test
    void testMapJavaTypeToJsonSchemaType() throws NoSuchMethodException {
        // This tests the internal mapping logic through input schema
        DynamicToolDef.ParameterDefinition stringParam = new DynamicToolDef.ParameterDefinition(
            "name", "java.lang.String", "", false, null
        );
        DynamicToolDef.ParameterDefinition intParam = new DynamicToolDef.ParameterDefinition(
            "count", "java.lang.Integer", "", false, null
        );
        DynamicToolDef.ParameterDefinition boolParam = new DynamicToolDef.ParameterDefinition(
            "enabled", "java.lang.Boolean", "", false, null
        );
        DynamicToolDef.ParameterDefinition doubleParam = new DynamicToolDef.ParameterDefinition(
            "price", "java.lang.Double", "", false, null
        );

        DynamicToolDef toolDef = new DynamicToolDef(
            "test", "", new Object(),
            getToStringMethod(),
            List.of(stringParam, intParam, boolParam, doubleParam)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) toolDef.getInputSchema().get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> nameSchema = (Map<String, Object>) properties.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> countSchema = (Map<String, Object>) properties.get("count");
        @SuppressWarnings("unchecked")
        Map<String, Object> enabledSchema = (Map<String, Object>) properties.get("enabled");
        @SuppressWarnings("unchecked")
        Map<String, Object> priceSchema = (Map<String, Object>) properties.get("price");

        assertEquals("string", nameSchema.get("type"));
        assertEquals("number", countSchema.get("type"));
        assertEquals("boolean", enabledSchema.get("type"));
        assertEquals("number", priceSchema.get("type"));
    }
}
