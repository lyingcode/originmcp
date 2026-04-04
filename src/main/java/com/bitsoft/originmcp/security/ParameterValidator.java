package com.bitsoft.originmcp.security;

import com.bitsoft.originmcp.dynamicregistry.DynamicToolDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates tool input parameters against their definitions.
 * Performs type checking, required field validation, and format validation.
 */
@Component
public class ParameterValidator {

    private static final Logger log = LoggerFactory.getLogger(ParameterValidator.class);

    /**
     * Validation result containing any errors found.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * Validates all parameters for a tool invocation.
     *
     * @param arguments Map of argument names to values
     * @param parameterDefinitions Ordered list of parameter definitions
     * @return ValidationResult indicating success or failure with error details
     */
    public ValidationResult validate(Map<String, Object> arguments,
                                    List<DynamicToolDef.ParameterDefinition> parameterDefinitions) {
        List<String> errors = new ArrayList<>();

        for (DynamicToolDef.ParameterDefinition paramDef : parameterDefinitions) {
            String paramName = paramDef.getName();
            Object value = arguments.get(paramName);

            // Check required
            if (value == null) {
                if (paramDef.isRequired()) {
                    errors.add("Required parameter missing: " + paramName);
                }
                continue;
            }

            // Type check
            String typeError = validateType(value, paramDef.getParameterType(), paramName);
            if (typeError != null) {
                errors.add(typeError);
            }

            // Format validation (if pattern is provided in description or via custom logic)
            String formatError = validateFormat(value, paramDef);
            if (formatError != null) {
                errors.add(formatError);
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }

        log.warn("Parameter validation failed: {}", errors);
        return ValidationResult.failure(errors);
    }

    /**
     * Validates the type of a parameter value against its expected type.
     */
    private String validateType(Object value, String expectedType, String paramName) {
        if (expectedType == null || expectedType.isEmpty()) {
            return null; // No type check possible
        }

        String typeName = expectedType.toLowerCase();

        if (value instanceof String) {
            // For String type, always valid
            if (typeName.contains("string") || typeName.contains("java.lang.string")) {
                return null;
            }
            // For numeric types, we allow string conversion later
            if (typeName.contains("int") || typeName.contains("long") ||
                typeName.contains("double") || typeName.contains("float") ||
                typeName.contains("number")) {
                // Try to validate if it's a valid numeric string
                try {
                    if (typeName.contains("int") || typeName.contains("long")) {
                        Long.parseLong((String) value);
                    } else {
                        Double.parseDouble((String) value);
                    }
                    return null;
                } catch (NumberFormatException e) {
                    return "Invalid numeric value for parameter '" + paramName + "': " + value;
                }
            }
            return null;
        }

        if (value instanceof Number) {
            if (typeName.contains("int") || typeName.contains("long")) {
                // Check if it's an integer
                if (value instanceof Byte || value instanceof Short ||
                    value instanceof Integer || value instanceof Long) {
                    return null;
                }
                // Allow conversion
                try {
                    ((Number) value).longValue();
                    return null;
                } catch (Exception e) {
                    return "Type mismatch for parameter '" + paramName + "': expected numeric integer";
                }
            }
            if (typeName.contains("double") || typeName.contains("float") ||
                typeName.contains("decimal")) {
                return null;
            }
        }

        if (value instanceof Boolean) {
            if (typeName.contains("boolean")) {
                return null;
            }
        }

        // For complex types or when type is uncertain, allow the value
        return null;
    }

    /**
     * Validates the format of a parameter value.
     * Currently supports regex patterns extracted from parameter description.
     */
    private String validateFormat(Object value, DynamicToolDef.ParameterDefinition paramDef) {
        // If description contains a pattern hint like "format: email" or "pattern: ^[a-z]+$"
        String description = paramDef.getDescription();
        if (description == null || description.isEmpty()) {
            return null;
        }

        // Extract pattern from description (simple heuristic)
        String pattern = extractPattern(description);
        if (pattern == null) {
            return null;
        }

        try {
            if (!Pattern.matches(pattern, value.toString())) {
                return "Format mismatch for parameter '" + paramDef.getName() +
                    "': value '" + value + "' does not match pattern '" + pattern + "'";
            }
        } catch (Exception e) {
            log.debug("Pattern validation skipped for param {}: {}", paramDef.getName(), e.getMessage());
        }

        return null;
    }

    /**
     * Extracts a regex pattern from parameter description.
     * Looks for patterns like "pattern: ^[a-z]+$" or "format: email" etc.
     */
    private String extractPattern(String description) {
        // Look for explicit pattern notation
        String lowerDesc = description.toLowerCase();

        // Email pattern
        if (lowerDesc.contains("format: email") || lowerDesc.contains("type: email")) {
            return "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        }

        // URL pattern
        if (lowerDesc.contains("format: url") || lowerDesc.contains("type: url")) {
            return "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        }

        // Phone pattern
        if (lowerDesc.contains("format: phone") || lowerDesc.contains("type: phone")) {
            return "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$";
        }

        // Explicit pattern
        int patternIndex = lowerDesc.indexOf("pattern:");
        if (patternIndex >= 0) {
            int start = patternIndex + 8;
            int end = description.indexOf(' ', start);
            if (end < 0) end = description.length();
            return description.substring(start, end).trim();
        }

        return null;
    }
}
