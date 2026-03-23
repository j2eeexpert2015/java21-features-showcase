package org.example.concepts.dynamicagentloading;

/**
 * JEP 451 Demo: Simple Calculator class
 *
 * Mockito's InlineByteBuddyMockMaker (default since Mockito 5)
 * uses bytecode instrumentation to mock any class, loading
 * ByteBuddy's agent dynamically at runtime via the Attach API.
 * This triggers JEP 451 warnings even on non-final classes.
 */
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }
}