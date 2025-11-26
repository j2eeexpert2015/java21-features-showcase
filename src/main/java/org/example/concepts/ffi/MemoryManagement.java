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
 * Requirements: Java 21+ with --enable-preview
 *                --enable-native-access=ALL-UNNAMED
 */
@SuppressWarnings("preview")
public class MemoryManagement {

    public static void main(String[] args) {
        System.out.println(
                "=== Demo 3: Memory Management ===\n");

        demoPrimitives();
        demoArrays();
        demoMemorySegmentTypes();
        demoArenaTypes();
        demoSafety();

        System.out.println("=== All Demos Complete ===");
    }

    // Example 1: Working with primitives
    private static void demoPrimitives() {
        System.out.println("Example 1: Primitives\n");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate: 4 bytes for int, 8 bytes for long
            MemorySegment intMem =
                    arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment longMem =
                    arena.allocate(ValueLayout.JAVA_LONG);

            // Write values
            intMem.set(ValueLayout.JAVA_INT, 0, 42);
            longMem.set(ValueLayout.JAVA_LONG, 0,
                    9876543210L);

            // Read and display with memory addresses
            System.out.printf("  int:  %d at 0x%x%n",
                    intMem.get(ValueLayout.JAVA_INT, 0),
                    intMem.address());
            System.out.printf("  long: %d at 0x%x%n",
                    longMem.get(ValueLayout.JAVA_LONG, 0),
                    longMem.address());
        } // Arena closes, memory freed automatically

        System.out.println("  ✓ Memory auto-freed\n");
    }

    // Example 2: Working with arrays
    private static void demoArrays() {
        System.out.println("Example 2: Arrays\n");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate array of 5 integers
            MemorySegment array =
                    arena.allocateArray(ValueLayout.JAVA_INT, 5);

            // Write: store squares (0, 1, 4, 9, 16)
            for (int i = 0; i < 5; i++) {
                array.setAtIndex(
                        ValueLayout.JAVA_INT, i, i * i);
            }

            // Read back as Java array
            int[] values =
                    array.toArray(ValueLayout.JAVA_INT);
            System.out.println("  Array: " +
                    Arrays.toString(values));

            // Modify first element
            array.setAtIndex(ValueLayout.JAVA_INT, 0, 999);
            System.out.printf("  After modifying [0]: %d%n",
                    array.getAtIndex(ValueLayout.JAVA_INT, 0));
        }

        System.out.println("  ✓ Memory auto-freed\n");
    }

    // Example 3: Memory segment types
    private static void demoMemorySegmentTypes() {
        System.out.println("Example 3: Segment Types\n");

        try (Arena arena = Arena.ofConfined()) {
            // Type 1: Native (off-heap)
            MemorySegment nativeSeg = arena.allocate(1024);
            System.out.println(
                    "  ✓ Native (off-heap memory)");

            // Type 2: Array (wrap existing Java array)
            MemorySegment arraySeg =
                    MemorySegment.ofArray(new int[]{1, 2, 3});
            System.out.println(
                    "  ✓ Array (wraps Java array)");

            // Type 3: Buffer (wrap ByteBuffer)
            MemorySegment bufferSeg =
                    MemorySegment.ofBuffer(
                            java.nio.ByteBuffer.allocate(64));
            System.out.println(
                    "  ✓ Buffer (wraps ByteBuffer)");

            // Type 4: Mapped (memory-mapped files)
            System.out.println(
                    "  ✓ Mapped (via FileChannel.map)\n");
        }
    }

    // Example 4: Arena types
    private static void demoArenaTypes() {
        System.out.println("Example 4: Arena Types\n");

        // Confined: single-threaded, best performance
        try (Arena confined = Arena.ofConfined()) {
            MemorySegment mem =
                    confined.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 123);
            System.out.printf("  Confined: %d " +
                            "(single-thread, best performance)%n",
                    mem.get(ValueLayout.JAVA_INT, 0));
        }

        // Shared: multi-threaded
        try (Arena shared = Arena.ofShared()) {
            MemorySegment mem =
                    shared.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 456);
            System.out.printf("  Shared:   %d " +
                            "(multi-thread safe)%n",
                    mem.get(ValueLayout.JAVA_INT, 0));
        }

        // Auto: GC-managed
        MemorySegment autoMem =
                Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
        autoMem.set(ValueLayout.JAVA_INT, 0, 789);
        System.out.printf("  Auto:     %d (GC-managed)%n",
                autoMem.get(ValueLayout.JAVA_INT, 0));

        System.out.println();
    }

    // Example 5: Safety features
    private static void demoSafety() {
        System.out.println("Example 5: Memory Safety\n");

        // Spatial bounds: prevents out-of-bounds access
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mem =
                    arena.allocate(ValueLayout.JAVA_INT);

            // Valid write
            mem.set(ValueLayout.JAVA_INT, 0, 42);
            System.out.println("  ✓ In-bounds write succeeded");

            // Invalid write (out of bounds)
            try {
                mem.set(ValueLayout.JAVA_INT, 4, 99);
            } catch (IndexOutOfBoundsException e) {
                System.out.println(
                        "  ✓ Out-of-bounds write prevented");
            }
        }

        // Temporal bounds: prevents use-after-free
        MemorySegment freed;
        try (Arena arena = Arena.ofConfined()) {
            freed = arena.allocate(ValueLayout.JAVA_INT);
        } // Memory freed here

        try {
            freed.get(ValueLayout.JAVA_INT, 0);
        } catch (IllegalStateException e) {
            System.out.println(
                    "  ✓ Use-after-free prevented");
        }

        System.out.println(
                "\n  Much safer than raw C pointers!\n");
    }
}