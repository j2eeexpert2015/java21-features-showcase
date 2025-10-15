package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.util.Arrays;

/**
 * Demo 3: Direct Memory Management
 *
 * Shows how to work with native memory (off-heap):
 *   - Allocating and accessing primitives
 *   - Working with arrays
 *   - Arena lifecycle management
 *   - Memory safety features
 *
 * Requirements: Java 21+ with --enable-preview --enable-native-access=ALL-UNNAMED
 */
@SuppressWarnings("preview")
public class MemoryManagement {

    public static void main(String[] args) {
        System.out.println("=== Demo 3: Memory Management ===\n");

        demoPrimitives();
        demoArrays();
        demoArenaTypes();
        demoSafety();
    }

    // Example 1: Working with primitives
    private static void demoPrimitives() {
        System.out.println("1. Primitives in Native Memory");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment intMem = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment longMem = arena.allocate(ValueLayout.JAVA_LONG);

            // Write
            intMem.set(ValueLayout.JAVA_INT, 0, 42);
            longMem.set(ValueLayout.JAVA_LONG, 0, 9876543210L);

            // Read
            System.out.printf("   int:  %d (at 0x%x)%n",
                    intMem.get(ValueLayout.JAVA_INT, 0),
                    intMem.address());
            System.out.printf("   long: %d (at 0x%x)%n",
                    longMem.get(ValueLayout.JAVA_LONG, 0),
                    longMem.address());
        }
        System.out.println("   ✓ Memory auto-freed\n");
    }

    // Example 2: Working with arrays
    private static void demoArrays() {
        System.out.println("2. Arrays in Native Memory");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate array
            MemorySegment array = arena.allocateArray(ValueLayout.JAVA_INT, 5);

            // Write using setAtIndex
            for (int i = 0; i < 5; i++) {
                array.setAtIndex(ValueLayout.JAVA_INT, i, i * i);
            }

            // Read back
            int[] values = array.toArray(ValueLayout.JAVA_INT);
            System.out.println("   Array: " + Arrays.toString(values));

            // Modify
            array.setAtIndex(ValueLayout.JAVA_INT, 0, 999);
            System.out.printf("   Modified: array[0] = %d%n",
                    array.getAtIndex(ValueLayout.JAVA_INT, 0));
        }
        System.out.println("   ✓ Memory auto-freed\n");
    }

    // Example 3: Arena types
    private static void demoArenaTypes() {
        System.out.println("3. Arena Types");

        // Confined: single-threaded, best performance
        try (Arena confined = Arena.ofConfined()) {
            MemorySegment mem = confined.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 123);
            System.out.println("   Confined: " + mem.get(ValueLayout.JAVA_INT, 0));
        }

        // Shared: multi-threaded
        try (Arena shared = Arena.ofShared()) {
            MemorySegment mem = shared.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 456);
            System.out.println("   Shared:   " + mem.get(ValueLayout.JAVA_INT, 0));
        }

        // Auto: GC-managed
        MemorySegment autoMem = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
        autoMem.set(ValueLayout.JAVA_INT, 0, 789);
        System.out.println("   Auto:     " + autoMem.get(ValueLayout.JAVA_INT, 0));
        System.out.println("   (Auto memory freed by GC later)\n");
    }

    // Example 4: Safety features
    private static void demoSafety() {
        System.out.println("4. Memory Safety");

        // Bounds checking
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mem = arena.allocate(ValueLayout.JAVA_INT); // 4 bytes

            mem.set(ValueLayout.JAVA_INT, 0, 42); // OK
            System.out.println("   ✓ In-bounds write: OK");

            try {
                mem.set(ValueLayout.JAVA_INT, 4, 99); // Out of bounds!
            } catch (IndexOutOfBoundsException e) {
                System.out.println("   ✓ Out-of-bounds prevented");
            }
        }

        // Use-after-free prevention
        MemorySegment freed;
        try (Arena arena = Arena.ofConfined()) {
            freed = arena.allocate(ValueLayout.JAVA_INT);
        } // Memory freed here

        try {
            freed.get(ValueLayout.JAVA_INT, 0); // Try to use freed memory
        } catch (IllegalStateException e) {
            System.out.println("   ✓ Use-after-free prevented");
        }

        System.out.println("\n✓ Much safer than raw C pointers!");
    }
}