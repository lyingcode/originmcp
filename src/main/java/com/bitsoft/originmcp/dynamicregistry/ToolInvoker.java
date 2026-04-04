package com.bitsoft.originmcp.dynamicregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles reflection-based invocation of tool methods on Spring beans.
 * Includes error handling and method caching for performance.
 */
@Component
public class ToolInvoker {
    private static final Logger log = LoggerFactory.getLogger(ToolInvoker.class);

    // Cache for resolved methods to avoid repeated lookups
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Invokes a tool method using reflection.
     *
     * @param serviceBean The Spring bean containing the method
     * @param methodName The name of the method to invoke
     * @param arguments Map of argument names to values
     * @param parameterDefinitions Ordered list of parameter definitions
     * @return The result of the method invocation
     * @throws ToolInvocationException if the invocation fails
     */
    public Object invoke(Object serviceBean, String methodName,
                        Map<String, Object> arguments,
                        java.util.List<DynamicToolDef.ParameterDefinition> parameterDefinitions)
            throws ToolInvocationException {

        String cacheKey = serviceBean.getClass().getName() + "." + methodName;
        Method method = methodCache.get(cacheKey);

        if (method == null) {
            method = resolveMethod(serviceBean.getClass(), methodName, parameterDefinitions);
            if (method == null) {
                throw new ToolInvocationException("Method not found: " + methodName +
                    " on bean " + serviceBean.getClass().getName());
            }
            methodCache.put(cacheKey, method);
        }

        try {
            // Build arguments array in correct order
            Object[] args = buildArguments(method, arguments, parameterDefinitions);

            log.debug("Invoking method: {}.{} with {} arguments",
                serviceBean.getClass().getSimpleName(), methodName, args.length);

            method.setAccessible(true);
            return method.invoke(serviceBean, args);

        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception
            Throwable cause = e.getCause();
            log.error("Tool invocation failed: {}.{} - {}",
                serviceBean.getClass().getSimpleName(), methodName, cause.getMessage(), cause);
            throw new ToolInvocationException("Tool method failed: " + cause.getMessage(), cause);

        } catch (Exception e) {
            log.error("Tool invocation error: {}.{} - {}",
                serviceBean.getClass().getSimpleName(), methodName, e.getMessage(), e);
            throw new ToolInvocationException("Failed to invoke tool: " + e.getMessage(), e);
        }
    }

    private Method resolveMethod(Class<?> beanClass, String methodName,
                                java.util.List<DynamicToolDef.ParameterDefinition> parameterDefinitions) {
        // Try exact parameter count match first
        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName) &&
                method.getParameterCount() == parameterDefinitions.size()) {
                return method;
            }
        }

        // Fallback: find method by name regardless of parameter count (use all matching methods)
        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() > 0) {
                return method;
            }
        }

        return null;
    }

    private Object[] buildArguments(Method method, Map<String, Object> arguments,
                                   java.util.List<DynamicToolDef.ParameterDefinition> parameterDefinitions) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            DynamicToolDef.ParameterDefinition paramDef = parameterDefinitions.get(i);
            Object argValue = arguments.get(paramDef.getName());

            if (argValue == null && paramDef.getDefaultValue() != null) {
                argValue = convertDefaultValue(paramDef.getDefaultValue(), paramTypes[i]);
            }

            if (argValue == null && paramDef.isRequired()) {
                throw new ToolInvocationException("Required parameter missing: " + paramDef.getName());
            }

            // Type conversion if necessary
            args[i] = convertValue(argValue, paramTypes[i]);
        }

        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        // Handle string to number conversions
        if (value instanceof String) {
            String strValue = (String) value;
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(strValue);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(strValue);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(strValue);
            }
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(strValue);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(strValue);
            }
        }

        return value;
    }

    private Object convertDefaultValue(String defaultValue, Class<?> targetType) {
        if (defaultValue == null || defaultValue.isEmpty()) return null;
        return convertValue(defaultValue, targetType);
    }

    /**
     * Exception thrown when tool invocation fails.
     */
    public static class ToolInvocationException extends RuntimeException {
        public ToolInvocationException(String message) {
            super(message);
        }

        public ToolInvocationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
