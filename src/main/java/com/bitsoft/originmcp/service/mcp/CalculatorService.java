package com.bitsoft.originmcp.service.mcp;

import org.springframework.stereotype.Service;

/**
 * Calculator service for MCP tool demonstration.
 * These methods are registered as MCP tools via data.sql.
 */
@Service
public class CalculatorService {

    /**
     * Add two numbers.
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * Subtract two numbers.
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Multiply two numbers.
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Divide two numbers.
     */
    public double divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return (double) a / b;
    }

    /**
     * Get greeting message.
     */
    public String greet(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, World!";
        }
        return "Hello, " + name + "!";
    }

    /**
     * Echo the input text.
     */
    public String echo(String text) {
        return text != null ? text : "";
    }

    /**
     * Get current timestamp.
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
