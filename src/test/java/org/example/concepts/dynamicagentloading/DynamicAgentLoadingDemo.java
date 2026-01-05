package org.example.concepts.dynamicagentloading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JEP 451 Demo: Dynamic Agent Loading
 *
 * WHAT HAPPENS:
 * =============
 * 1. Mockito detects Calculator is final
 * 2. Cannot create subclass (traditional approach)
 * 3. Switches to ByteBuddy inline mock maker
 * 4. ByteBuddy calls: ByteBuddyAgent.install()
 * 5. Internally executes:
 *      VirtualMachine vm = VirtualMachine.attach(currentPID);
 *      vm.loadAgent("byte-buddy-agent.jar");  ← DYNAMIC LOADING
 *      vm.detach();
 * 6. JVM detects dynamic loading
 * 7. ⚠️ JEP 451 WARNING appears
 *
 * RUN THIS:
 * =========
 * mvn clean test -Dtest=DynamicAgentLoadingDemo
 *
 * RESULT: You'll see the WARNING in console output
 *
 * TO SUPPRESS:
 * ============
 * Add to pom.xml surefire plugin:
 * <argLine>-XX:+EnableDynamicAgentLoading</argLine>
 */
@ExtendWith(MockitoExtension.class)
class DynamicAgentLoadingDemo {

    /**
     * ⚠️ This @Mock on a FINAL class triggers dynamic agent loading
     */
    @Mock
    private Calculator calculator;

    @Test
    void demonstrateDynamicAgentLoading() {
        // Watch console for JEP 451 warning when this test runs

        // Arrange
        when(calculator.add(2, 3)).thenReturn(100);
        when(calculator.multiply(4, 5)).thenReturn(200);

        // Act & Assert
        assertEquals(100, calculator.add(2, 3));
        assertEquals(200, calculator.multiply(4, 5));

        verify(calculator).add(2, 3);
        verify(calculator).multiply(4, 5);
    }

    @Test
    void anotherTestShowingSameWarning() {
        // The warning appears once per test run, not per test method
        // Once ByteBuddy's agent is loaded, it stays for all tests

        when(calculator.add(anyInt(), anyInt())).thenReturn(999);

        assertEquals(999, calculator.add(1, 2));
        assertEquals(999, calculator.add(5, 10));
    }
}