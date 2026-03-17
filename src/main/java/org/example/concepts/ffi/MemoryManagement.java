package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.util.Arrays;

/*
 * Demo 3: Direct Memory Management
 * Shows how to work with native memory (off-heap) using the FFM API
 *   - Allocating and accessing primitives
 *   - Working with arrays
 *   - Segment types, Arena types, Memory safety
 *
 * Requirements: Java 21+ with --enable-preview --enable-native-access=ALL-UNNAMED
 */
@SuppressWarnings("preview")
public class MemoryManagement {

    public static void main(String[] args) {
        System.out.println("=== Demo 3: Memory Management ===\n");
        demoPrimitives();
        demoArrays();
        demoMemorySegmentTypes();
        demoArenaTypes();
        demoSafety();
        System.out.println("=== All Demos Complete ===");
    }

    /*
     * Example 1: Allocate primitives off-heap
     * Arena manages lifecycle — memory freed automatically on close
     */
    private static void demoPrimitives() {
        System.out.println("--- Example 1: Primitives ---");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate 4 bytes for int, 8 bytes for long
            MemorySegment intMem  = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment longMem = arena.allocate(ValueLayout.JAVA_LONG);

            // Write values
            intMem.set(ValueLayout.JAVA_INT,   0, 42);
            longMem.set(ValueLayout.JAVA_LONG, 0, 9876543210L);

            // Read and print — shows value and actual native address
            System.out.println("  [Allocated 4 bytes off-heap for int]");
            System.out.printf("  int  : %d at native address 0x%x%n",
                    intMem.get(ValueLayout.JAVA_INT, 0), intMem.address());

            System.out.println("  [Allocated 8 bytes off-heap for long]");
            System.out.printf("  long : %d at native address 0x%x%n",
                    longMem.get(ValueLayout.JAVA_LONG, 0), longMem.address());

        } // Arena closes → memory freed

        System.out.println("  [Arena closed — memory freed automatically]");
        System.out.println("  Memory auto-freed\n");
    }

    /*
     * Example 2: Allocate and access a native int array
     * setAtIndex / getAtIndex for indexed access — like array[i] in C
     */
    private static void demoArrays() {
        System.out.println("--- Example 2: Arrays ---");

        try (Arena arena = Arena.ofConfined()) {
            // 20 bytes — 5 ints contiguous in native memory
            System.out.println("  [Allocated 20 bytes off-heap — 5 contiguous ints]");
            MemorySegment array = arena.allocateArray(ValueLayout.JAVA_INT, 5);

            // Write squares: 0, 1, 4, 9, 16
            for (int i = 0; i < 5; i++)
                array.setAtIndex(ValueLayout.JAVA_INT, i, i * i);

            // Read back as Java array
            int[] values = array.toArray(ValueLayout.JAVA_INT);
            System.out.println("  Array        : " + Arrays.toString(values));

            // Modify and verify — change happens directly in native memory
            System.out.println("  [Modifying index [0] directly in native memory]");
            array.setAtIndex(ValueLayout.JAVA_INT, 0, 999);
            System.out.println("  After [0]=999 : "
                    + array.getAtIndex(ValueLayout.JAVA_INT, 0));
        }

        System.out.println("  [Arena closed — memory freed automatically]");
        System.out.println("  Memory auto-freed\n");
    }

    /*
     * Example 3: Three ways to obtain a MemorySegment
     * Native (off-heap), Array (Java heap), Buffer (ByteBuffer)
     */
    private static void demoMemorySegmentTypes() {
        System.out.println("--- Example 3: Segment Types ---");
        System.out.println("  [MemorySegment works with three different backing sources]");

        try (Arena arena = Arena.ofConfined()) {
            // Native — allocated off-heap
            MemorySegment nativeSeg = arena.allocate(1024);
            System.out.println("  Native  : off-heap memory");

            // Array — wraps existing Java array
            MemorySegment arraySeg = MemorySegment.ofArray(new int[]{1, 2, 3});
            System.out.println("  Array   : wraps Java array");

            // Buffer — wraps ByteBuffer
            MemorySegment bufferSeg = MemorySegment.ofBuffer(
                    java.nio.ByteBuffer.allocate(64));
            System.out.println("  Buffer  : wraps ByteBuffer");

            // Mapped — memory-mapped file (via FileChannel.map)
            System.out.println("  Mapped  : via FileChannel.map");
        }

        System.out.println("  [Same MemorySegment API regardless of source]\n");
    }

    /*
     * Example 4: Three Arena types — control lifecycle and threading
     * Confined (single-thread), Shared (multi-thread), Auto (GC-managed)
     */
    private static void demoArenaTypes() {
        System.out.println("--- Example 4: Arena Types ---");
        System.out.println("  [Choose Arena type based on threading and lifecycle needs]");

        try (Arena confined = Arena.ofConfined()) {
            MemorySegment mem = confined.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 123);
            System.out.printf("  Confined : %d (single-thread, best performance)%n",
                    mem.get(ValueLayout.JAVA_INT, 0));
        }

        try (Arena shared = Arena.ofShared()) {
            MemorySegment mem = shared.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 456);
            System.out.printf("  Shared   : %d (multi-thread safe)%n",
                    mem.get(ValueLayout.JAVA_INT, 0));
        }

        MemorySegment autoMem = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
        autoMem.set(ValueLayout.JAVA_INT, 0, 789);
        System.out.printf("  Auto     : %d (GC-managed)%n%n",
                autoMem.get(ValueLayout.JAVA_INT, 0));
    }

    /*
     * Example 5: Memory safety — spatial and temporal bounds enforced
     * Out-of-bounds → IndexOutOfBoundsException
     * Use-after-free → IllegalStateException
     */
    private static void demoSafety() {
        System.out.println("--- Example 5: Memory Safety ---");

        // Spatial bounds check
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mem = arena.allocate(ValueLayout.JAVA_INT);
            mem.set(ValueLayout.JAVA_INT, 0, 42);

            System.out.println("  [Spatial safety — write within allocated 4 bytes]");
            System.out.println("  In-bounds write       : OK");

            try {
                System.out.println("  [Spatial safety — write beyond allocated 4 bytes]");
                mem.set(ValueLayout.JAVA_INT, 4, 99);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("  Out-of-bounds write   : IndexOutOfBoundsException");
            }
        }

        // Temporal bounds check
        MemorySegment freed;
        try (Arena arena = Arena.ofConfined()) {
            freed = arena.allocate(ValueLayout.JAVA_INT);
        } // Arena closed → memory freed

        try {
            System.out.println("  [Temporal safety — access after Arena is closed]");
            freed.get(ValueLayout.JAVA_INT, 0);
        } catch (IllegalStateException e) {
            System.out.println("  Use-after-free access : IllegalStateException");
        }

        System.out.println("  [No JVM crash — FFM enforces spatial and temporal safety]\n");
    }
}