package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Demonstrates direct native memory manipulation using the Foreign Function & Memory API.
 *
 * This example shows:
 * - Allocating native memory (off-heap, not managed by GC)
 * - Reading and writing primitive values
 * - Working with arrays in native memory
 * - String manipulation in native memory
 * - Different Arena types and their lifecycle management
 * - Memory safety features
 *
 * Native memory is NOT managed by Java's garbage collector and exists outside the heap.
 * This is essential for:
 * - Interacting with native libraries (they need stable memory addresses)
 * - Large datasets that would overwhelm the heap
 * - Performance-critical operations (no GC overhead)
 * - Direct hardware access
 *
 * Requirements: Java 21 with --enable-preview and --enable-native-access=ALL-UNNAMED
 *
 * Run: java --enable-preview --enable-native-access=ALL-UNNAMED
 *      org.example.concepts.ffi.MemoryManipulationExample
 */
@SuppressWarnings("preview")
public class MemoryManipulationExample {

    public static void main(String[] args) {
        System.out.println("=== Foreign Function & Memory API - Memory Manipulation ===");
        System.out.println("Demonstrating direct native memory operations\n");

        // Example 1: Working with primitive values
        demonstratePrimitives();

        // Example 2: Working with arrays
        demonstrateArrays();

        // Example 3: Working with strings
        demonstrateStrings();

        // Example 4: Arena lifecycle management
        demonstrateArenaTypes();

        // Example 5: Memory safety features
        demonstrateMemorySafety();

        System.out.println("\n=== Summary ===");
        System.out.println("✓ Native memory is fast and powerful");
        System.out.println("✓ Arena pattern prevents memory leaks");
        System.out.println("✓ Bounds checking prevents memory corruption");
        System.out.println("✓ Perfect for native interop and performance-critical code");
    }

    /**
     * Example 1: Working with primitive values in native memory
     */
    private static void demonstratePrimitives() {
        System.out.println("=== Example 1: Primitive Values ===");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for different primitive types
            MemorySegment intSegment = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment longSegment = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment doubleSegment = arena.allocate(ValueLayout.JAVA_DOUBLE);

            System.out.println("✓ Allocated memory for int, long, and double");

            // Write values to native memory
            intSegment.set(ValueLayout.JAVA_INT, 0, 42);
            longSegment.set(ValueLayout.JAVA_LONG, 0, 9876543210L);
            doubleSegment.set(ValueLayout.JAVA_DOUBLE, 0, 3.14159);

            System.out.println("✓ Written values to native memory");

            // Read values back
            int intValue = intSegment.get(ValueLayout.JAVA_INT, 0);
            long longValue = longSegment.get(ValueLayout.JAVA_LONG, 0);
            double doubleValue = doubleSegment.get(ValueLayout.JAVA_DOUBLE, 0);

            System.out.println("\nValues read from native memory:");
            System.out.println("  Int:    " + intValue);
            System.out.println("  Long:   " + longValue);
            System.out.println("  Double: " + doubleValue);

            // Show memory addresses (these are real native pointers!)
            System.out.println("\nMemory addresses:");
            System.out.println("  Int:    0x" + Long.toHexString(intSegment.address()));
            System.out.println("  Long:   0x" + Long.toHexString(longSegment.address()));
            System.out.println("  Double: 0x" + Long.toHexString(doubleSegment.address()));

        } // Memory automatically freed when arena closes

        System.out.println("\n✓ Memory automatically freed (Arena closed)\n");
    }

    /**
     * Example 2: Working with arrays in native memory
     */
    private static void demonstrateArrays() {
        System.out.println("=== Example 2: Array Operations ===");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate an array of 10 integers
            int arraySize = 10;
            MemorySegment arraySegment = arena.allocateArray(ValueLayout.JAVA_INT, arraySize);

            System.out.println("✓ Allocated array of " + arraySize + " integers in native memory");

            // Write values using setAtIndex (convenient for arrays)
            System.out.println("\nWriting values: squares of indices");
            for (int i = 0; i < arraySize; i++) {
                arraySegment.setAtIndex(ValueLayout.JAVA_INT, i, i * i);
            }

            // Read values back
            System.out.print("Array contents: [");
            for (int i = 0; i < arraySize; i++) {
                int value = arraySegment.getAtIndex(ValueLayout.JAVA_INT, i);
                System.out.print(value);
                if (i < arraySize - 1) System.out.print(", ");
            }
            System.out.println("]");

            // Alternative: Copy entire array to Java array
            int[] javaArray = arraySegment.toArray(ValueLayout.JAVA_INT);
            System.out.println("Copied to Java array: " + Arrays.toString(javaArray));

            // Modify some values
            arraySegment.setAtIndex(ValueLayout.JAVA_INT, 0, 999);
            arraySegment.setAtIndex(ValueLayout.JAVA_INT, 9, 888);

            System.out.println("\nModified first and last elements:");
            System.out.println("  array[0] = " + arraySegment.getAtIndex(ValueLayout.JAVA_INT, 0));
            System.out.println("  array[9] = " + arraySegment.getAtIndex(ValueLayout.JAVA_INT, 9));

            // Show byte-level access (advanced)
            System.out.println("\nByte-level view of first integer (999 in little-endian):");
            for (int i = 0; i < 4; i++) {  // int is 4 bytes
                byte b = arraySegment.get(ValueLayout.JAVA_BYTE, i);
                System.out.printf("  Byte %d: 0x%02X (%d)%n", i, b & 0xFF, b & 0xFF);
            }

        }

        System.out.println("\n✓ Array memory automatically freed\n");
    }

    /**
     * Example 3: Working with strings in native memory
     */
    private static void demonstrateStrings() {
        System.out.println("=== Example 3: String Operations ===");

        try (Arena arena = Arena.ofConfined()) {
            // Create a native string (null-terminated, UTF-8 encoded)
            String javaString = "Hello, FFM API! 🚀";
            MemorySegment nativeString = arena.allocateUtf8String(javaString);

            System.out.println("Original Java string: \"" + javaString + "\"");
            System.out.println("✓ Allocated in native memory as null-terminated UTF-8");

            // Read it back
            String readBack = nativeString.getUtf8String(0);
            System.out.println("Read back from native: \"" + readBack + "\"");
            System.out.println("Strings match: " + javaString.equals(readBack));

            // Show the raw bytes (UTF-8 encoding)
            byte[] utf8Bytes = javaString.getBytes(StandardCharsets.UTF_8);
            System.out.println("\nUTF-8 byte representation:");
            System.out.print("  Bytes: [");
            for (int i = 0; i < Math.min(utf8Bytes.length, 20); i++) {  // Show first 20 bytes
                System.out.printf("0x%02X", utf8Bytes[i] & 0xFF);
                if (i < Math.min(utf8Bytes.length, 20) - 1) System.out.print(", ");
            }
            if (utf8Bytes.length > 20) System.out.print(", ...");
            System.out.println("]");

            System.out.println("  Total bytes: " + utf8Bytes.length);
            System.out.println("  Java length: " + javaString.length() + " characters");
            System.out.println("  Note: UTF-8 uses multiple bytes for emoji (🚀 = 4 bytes)");

            // Demonstrate manual string building in native memory
            System.out.println("\nManual string construction:");
            String manual = "ABC";
            MemorySegment manualString = arena.allocate(4);  // 3 chars + null terminator
            manualString.set(ValueLayout.JAVA_BYTE, 0, (byte) 'A');
            manualString.set(ValueLayout.JAVA_BYTE, 1, (byte) 'B');
            manualString.set(ValueLayout.JAVA_BYTE, 2, (byte) 'C');
            manualString.set(ValueLayout.JAVA_BYTE, 3, (byte) 0);  // Null terminator

            String manualRead = manualString.getUtf8String(0);
            System.out.println("  Manually constructed: \"" + manualRead + "\"");

        }

        System.out.println("\n✓ String memory automatically freed\n");
    }

    /**
     * Example 4: Different Arena types and their lifecycle management
     */
    private static void demonstrateArenaTypes() {
        System.out.println("=== Example 4: Arena Lifecycle Management ===");

        // Type 1: Global Arena (never freed, lives forever)
        System.out.println("\n1. Global Arena:");
        System.out.println("   - Memory lives for entire program duration");
        System.out.println("   - Use for: initial setup, permanent data");

        MemorySegment globalMemory = Arena.global().allocate(ValueLayout.JAVA_INT);
        globalMemory.set(ValueLayout.JAVA_INT, 0, 123);
        System.out.println("   ✓ Allocated global memory with value: " +
                globalMemory.get(ValueLayout.JAVA_INT, 0));
        System.out.println("   ℹ This memory will NEVER be freed (intentional)");

        // Type 2: Confined Arena (single-threaded, explicit control)
        System.out.println("\n2. Confined Arena:");
        System.out.println("   - Can only be used by one thread");
        System.out.println("   - Freed when arena.close() is called");
        System.out.println("   - Best performance for single-threaded code");

        try (Arena confined = Arena.ofConfined()) {
            MemorySegment confinedMemory = confined.allocate(ValueLayout.JAVA_INT);
            confinedMemory.set(ValueLayout.JAVA_INT, 0, 456);
            System.out.println("   ✓ Allocated confined memory: " +
                    confinedMemory.get(ValueLayout.JAVA_INT, 0));
        } // Automatically freed here
        System.out.println("   ✓ Confined memory freed when try-with-resources closed");

        // Type 3: Shared Arena (multi-threaded, explicit control)
        System.out.println("\n3. Shared Arena:");
        System.out.println("   - Can be used by multiple threads");
        System.out.println("   - Freed when arena.close() is called");
        System.out.println("   - Slightly slower than confined, but thread-safe");

        try (Arena shared = Arena.ofShared()) {
            MemorySegment sharedMemory = shared.allocate(ValueLayout.JAVA_INT);
            sharedMemory.set(ValueLayout.JAVA_INT, 0, 789);
            System.out.println("   ✓ Allocated shared memory: " +
                    sharedMemory.get(ValueLayout.JAVA_INT, 0));
        }
        System.out.println("   ✓ Shared memory freed");

        // Type 4: Auto Arena (garbage collected)
        System.out.println("\n4. Auto Arena:");
        System.out.println("   - Memory freed when GC collects the MemorySegment");
        System.out.println("   - Most Java-like, but less predictable timing");
        System.out.println("   - Use when you don't need precise control");

        MemorySegment autoMemory = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
        autoMemory.set(ValueLayout.JAVA_INT, 0, 999);
        System.out.println("   ✓ Allocated auto memory: " +
                autoMemory.get(ValueLayout.JAVA_INT, 0));
        System.out.println("   ℹ Will be freed when GC runs (non-deterministic)");

        System.out.println("\n=== Arena Comparison ===");
        System.out.println("┌─────────────┬──────────────┬─────────────┬──────────────┐");
        System.out.println("│ Arena Type  │ Thread-Safe  │ Cleanup     │ Use Case     │");
        System.out.println("├─────────────┼──────────────┼─────────────┼──────────────┤");
        System.out.println("│ Global      │ Yes          │ Never       │ Permanent    │");
        System.out.println("│ Confined    │ No           │ Explicit    │ Performance  │");
        System.out.println("│ Shared      │ Yes          │ Explicit    │ Multi-thread │");
        System.out.println("│ Auto        │ Yes          │ GC-based    │ Convenience  │");
        System.out.println("└─────────────┴──────────────┴─────────────┴──────────────┘");

        System.out.println();
    }

    /**
     * Example 5: Memory safety features
     */
    private static void demonstrateMemorySafety() {
        System.out.println("=== Example 5: Memory Safety Features ===");

        // Safety Feature 1: Bounds checking
        System.out.println("\n1. Bounds Checking:");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(ValueLayout.JAVA_INT);  // Only 4 bytes

            System.out.println("   ✓ Allocated 4 bytes (1 integer)");

            // This is safe
            segment.set(ValueLayout.JAVA_INT, 0, 42);
            System.out.println("   ✓ Writing at offset 0: OK");

            // This will throw an exception
            try {
                segment.set(ValueLayout.JAVA_INT, 4, 99);  // Out of bounds!
                System.out.println("   ✗ Writing at offset 4: Should have failed!");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("   ✓ Writing at offset 4: Correctly prevented! (" +
                        e.getClass().getSimpleName() + ")");
            }
        }

        // Safety Feature 2: Use-after-free prevention
        System.out.println("\n2. Use-After-Free Prevention:");
        MemorySegment freedSegment;
        try (Arena arena = Arena.ofConfined()) {
            freedSegment = arena.allocate(ValueLayout.JAVA_INT);
            freedSegment.set(ValueLayout.JAVA_INT, 0, 100);
            System.out.println("   ✓ Allocated and wrote value: " +
                    freedSegment.get(ValueLayout.JAVA_INT, 0));
        } // Arena closed, memory freed

        System.out.println("   ℹ Arena closed, memory freed");

        try {
            freedSegment.get(ValueLayout.JAVA_INT, 0);  // Try to use freed memory
            System.out.println("   ✗ Reading freed memory: Should have failed!");
        } catch (IllegalStateException e) {
            System.out.println("   ✓ Reading freed memory: Correctly prevented! (" +
                    e.getClass().getSimpleName() + ")");
        }

        // Safety Feature 3: Type safety with layouts
        System.out.println("\n3. Type Safety:");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(ValueLayout.JAVA_INT);  // 4 bytes

            segment.set(ValueLayout.JAVA_INT, 0, 42);
            System.out.println("   ✓ Writing int (4 bytes): OK");

            try {
                segment.set(ValueLayout.JAVA_LONG, 0, 123L);  // Trying to write 8 bytes!
                System.out.println("   ✗ Writing long (8 bytes): Should have failed!");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("   ✓ Writing long (8 bytes): Correctly prevented! (" +
                        e.getClass().getSimpleName() + ")");
            }
        }

        System.out.println("\n=== Safety Summary ===");
        System.out.println("✓ All memory accesses are bounds-checked");
        System.out.println("✓ Cannot use memory after it's been freed");
        System.out.println("✓ Type layouts prevent incorrect access patterns");
        System.out.println("✓ Much safer than raw C pointers!");
    }
}