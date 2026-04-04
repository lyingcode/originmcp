package com.bitsoft.originmcp.dynamicregistry;

import com.bitsoft.originmcp.security.McpAuthorizationService;
import com.bitsoft.originmcp.security.ParameterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates proxy wrappers around service beans to enforce security checks
 * before method invocations for MCP tools.
 * Handles both authorization and input validation.
 */
@Component
public class ToolSecurityInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ToolSecurityInterceptor.class);

    @Autowired
    private McpAuthorizationService authorizationService;

    @Autowired
    private ParameterValidator parameterValidator;

    // Map of service bean class -> wrapped proxy
    private final Map<Class<?>, Object> wrappedBeans = new ConcurrentHashMap<>();

    // Map of method name -> tool name (for authorization)
    private final Map<String, String> methodToToolMap = new ConcurrentHashMap<>();

    // Map of method name -> parameter definitions (for validation)
    private final Map<String, List<DynamicToolDef.ParameterDefinition>> methodToParamsMap =
        new ConcurrentHashMap<>();

    /**
     * Wrap a service bean with a security proxy if not already wrapped.
     * Only methods that are registered as MCP tools will be intercepted.
     *
     * @param bean The original service bean
     * @param beanName The Spring bean name
     * @param toolName The MCP tool name associated with this bean
     * @return A proxy that wraps the original bean with security checks
     */
    @SuppressWarnings("unchecked")
    public <T> T wrapIfNeeded(T bean, String beanName, String toolName) {
        Class<?> interfaceClass = bean.getClass();

        // Check if already wrapped
        if (wrappedBeans.containsKey(interfaceClass)) {
            return (T) wrappedBeans.get(interfaceClass);
        }

        // Create proxy
        Object proxy = Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            interfaceClass.getInterfaces(),
            new SecurityInvocationHandler(bean, toolName)
        );

        wrappedBeans.put(interfaceClass, proxy);
        log.debug("Created security proxy for bean: {} (tool: {})", beanName, toolName);

        return (T) proxy;
    }

    /**
     * Register a method as an MCP tool for authorization and validation.
     */
    public void registerToolMethod(String methodName, String toolName,
                                   List<DynamicToolDef.ParameterDefinition> parameterDefinitions) {
        methodToToolMap.put(methodName, toolName);
        if (parameterDefinitions != null) {
            methodToParamsMap.put(methodName, parameterDefinitions);
        }
    }

    /**
     * Get the MCP tool name for a method, if registered.
     */
    public String getToolNameForMethod(String methodName) {
        return methodToToolMap.get(methodName);
    }

    /**
     * Get parameter definitions for a method.
     */
    public List<DynamicToolDef.ParameterDefinition> getParameterDefinitions(String methodName) {
        return methodToParamsMap.getOrDefault(methodName, Collections.emptyList());
    }

    /**
     * Invocation handler that adds security checks around method invocations.
     */
    private class SecurityInvocationHandler implements InvocationHandler {
        private final Object target;
        private final String toolName;

        SecurityInvocationHandler(Object target, String toolName) {
            this.target = target;
            this.toolName = toolName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Get the effective tool name (method-specific or fallback to default)
            String effectiveToolName = methodToToolMap.getOrDefault(methodName, toolName);

            // 1. Authorization check - default deny
            if (!authorizationService.hasPermission(effectiveToolName)) {
                log.warn("Security: Access denied to tool '{}' via method '{}'",
                    effectiveToolName, methodName);
                throw new SecurityException("Access denied: tool '" + effectiveToolName + "' is not permitted");
            }

            // 2. Input validation
            List<DynamicToolDef.ParameterDefinition> paramDefs = methodToParamsMap.get(methodName);
            if (paramDefs != null && !paramDefs.isEmpty()) {
                // Build argument map from method parameters
                Map<String, Object> argMap = buildArgumentMap(method, args, paramDefs);

                ParameterValidator.ValidationResult validationResult =
                    parameterValidator.validate(argMap, paramDefs);

                if (!validationResult.isValid()) {
                    log.warn("Security: Parameter validation failed for tool '{}': {}",
                        effectiveToolName, validationResult.getErrorMessage());
                    throw new SecurityException("Invalid parameters: " + validationResult.getErrorMessage());
                }
            }

            // Proceed with actual method invocation
            try {
                return method.invoke(target, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Unwrap the actual exception
                throw e.getCause();
            }
        }

        /**
         * Build a map of argument name -> value from method parameters.
         */
        private Map<String, Object> buildArgumentMap(Method method, Object[] args,
                                                     List<DynamicToolDef.ParameterDefinition> paramDefs) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            if (args == null || args.length == 0) {
                return map;
            }

            for (int i = 0; i < paramDefs.size() && i < args.length; i++) {
                map.put(paramDefs.get(i).getName(), args[i]);
            }
            return map;
        }
    }
}
