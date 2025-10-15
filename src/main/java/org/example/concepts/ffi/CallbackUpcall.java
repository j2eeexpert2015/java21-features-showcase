package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * Demo 2: Callbacks (Upcalls) - Native code calling Java
 *
 * Uses C's qsort() to sort an array, but the comparison logic is in Java.
 * This shows bidirectional calling:
 *   Downcall: Java -> qsort()
 *   Upcall:   qsort() -> Java comparator
 *
 * Requirements: Java 21+ with --enable-preview --enable-native-access=ALL-UNNAMED
 */
@SuppressWarnings("preview")
public class CallbackUpcall {

    // Our Java comparator - will be called by C code
    private static int compare(MemorySegment a, MemorySegment b) {
        int valueA = a.get(ValueLayout.JAVA_INT, 0);
        int valueB = b.get(ValueLayout.JAVA_INT, 0);
        return Integer.compare(valueA, valueB);
    }

    public static void main(String[] args) {
        System.out.println("=== Demo 2: Callbacks (Upcalls) ===");
        System.out.println("Native qsort() calling our Java comparator\n");

        try {
            // Step 1-3: Get linker, lookup stdlib, find qsort
            Linker linker = Linker.nativeLinker();
            SymbolLookup stdlib = linker.defaultLookup();
            MemorySegment qsortAddr = stdlib.find("qsort")
                    .orElseThrow(() -> new RuntimeException("qsort not found"));

            // Step 4: Define qsort signature
            // C: void qsort(void *base, size_t nel, size_t width, int (*compar)(...))
            FunctionDescriptor qsortDesc = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,    // void *base
                    ValueLayout.JAVA_LONG,  // size_t nel
                    ValueLayout.JAVA_LONG,  // size_t width
                    ValueLayout.ADDRESS     // comparator function pointer
            );

            // Step 5: Create downcall handle for qsort
            MethodHandle qsort = linker.downcallHandle(qsortAddr, qsortDesc);

            // Step 6: Create upcall stub for our Java comparator
            MethodHandle compareHandle = MethodHandles.lookup()
                    .findStatic(CallbackUpcall.class, "compare",
                            MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class));

            FunctionDescriptor compareDesc = FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT),
                    ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT)
            );

            try (Arena arena = Arena.ofConfined()) {
                // Create the upcall stub (native function that calls Java)
                MemorySegment comparatorStub = linker.upcallStub(
                        compareHandle, compareDesc, arena);

                // Step 7: Prepare data
                int[] unsorted = {42, 7, 23, 91, 5, 68, 13, 99, 1, 56};
                System.out.println("Unsorted: " + Arrays.toString(unsorted));

                MemorySegment nativeArray = arena.allocateArray(ValueLayout.JAVA_INT, unsorted);

                // Step 8: Call qsort - it will call our Java comparator!
                System.out.println("\nCalling qsort()...");
                qsort.invoke(
                        nativeArray,
                        (long) unsorted.length,
                        (long) ValueLayout.JAVA_INT.byteSize(),
                        comparatorStub
                );

                // Step 9: Read sorted results
                int[] sorted = nativeArray.toArray(ValueLayout.JAVA_INT);
                System.out.println("Sorted:   " + Arrays.toString(sorted));

                System.out.println("\n✓ C code successfully called our Java comparator!");
            }

        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}