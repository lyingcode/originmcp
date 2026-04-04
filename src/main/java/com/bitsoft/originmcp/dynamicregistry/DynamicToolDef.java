package com.bitsoft.originmcp.dynamicregistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime representation of a tool ready for registration with Spring AI.
 */
public class DynamicToolDef {
    private final String name;
    private final String description;
    private final Object serviceBean;
    private final java.lang.reflect.Method method;
    private final List<ParameterDefinition> parameters;

    public DynamicToolDef(String name, String description, Object serviceBean,
                         java.lang.reflect.Method method, List<ParameterDefinition> parameters) {
        this.name = name;
        this.description = description;
        this.serviceBean = serviceBean;
        this.method = method;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    public java.lang.reflect.Method getMethod() {
        return method;
    }

    public List<ParameterDefinition> getParameters() {
        return parameters;
    }

    /**
     * Generates the JSON Schema for this tool's input parameters.
     */
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (ParameterDefinition param : parameters) {
            Map<String, Object> paramSchema = new LinkedHashMap<>();
            paramSchema.put("type", mapJavaTypeToJsonSchemaType(param.getParameterType()));
            paramSchema.put("description", param.getDescription());
            properties.put(param.getName(), paramSchema);
            if (param.isRequired()) {
                required.add(param.getName());
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

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
     * Parameter definition for a tool.
     */
    public static class ParameterDefinition {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final String defaultValue;

        public ParameterDefinition(String name, String type, String description,
                                  boolean required, String defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getParameterType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }
}
