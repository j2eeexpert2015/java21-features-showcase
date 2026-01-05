package org.example.concepts.dynamicagentloading;

/**
 * JEP 451 Demo: Simple final class
 *
 * The 'final' keyword prevents Mockito from using traditional
 * subclass-based mocking, forcing it to dynamically load
 * ByteBuddy's agent at runtime.
 */
public final class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }
}