package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * Demonstrates callbacks (upcalls) - native code calling back into Java code.
 *
 * This example uses C's qsort() function to sort an array of integers.
 * The sorting logic (comparison) is implemented in Java, but called by native C code.
 *
 * This shows the bidirectional nature of FFM API:
 * - Downcall: Java -> Native (qsort function)
 * - Upcall: Native -> Java (our comparator callback)
 *
 * Requirements: Java 21 with --enable-preview and --enable-native-access=ALL-UNNAMED
 *
 * Run: java --enable-preview --enable-native-access=ALL-UNNAMED
 *      org.example.concepts.ffi.QSortCallbackExample
 */
@SuppressWarnings("preview")
public class QSortCallbackExample {

    /**
     * Our Java comparator function that will be called by C's qsort().
     *
     * This method receives two memory addresses pointing to integers,
     * reads the values, and returns the comparison result.
     *
     * @param a Pointer to first integer
     * @param b Pointer to second integer
     * @return Negative if a < b, zero if a == b, positive if a > b
     */
    private static int compareIntegers(MemorySegment a, MemorySegment b) {
        // Read the integer values from native memory
        int valueA = a.get(ValueLayout.JAVA_INT, 0);
        int valueB = b.get(ValueLayout.JAVA_INT, 0);

        // Compare them (ascending order)
        return Integer.compare(valueA, valueB);
    }

    public static void main(String[] args) {
        System.out.println("=== Callbacks (Upcalls) Example with qsort() ===");
        System.out.println("Demonstrating native code calling back into Java\n");

        try {
            // Step 1: Get the native linker
            Linker linker = Linker.nativeLinker();
            System.out.println("✓ Native linker obtained");

            // Step 2: Lookup the C standard library
            SymbolLookup stdlib = linker.defaultLookup();
            System.out.println("✓ C standard library loaded");

            // Step 3: Find the qsort function
            // C signature: void qsort(void *base, size_t nel, size_t width,
            //                         int (*compar)(const void *, const void *))
            MemorySegment qsortAddress = stdlib.find("qsort")
                    .orElseThrow(() -> new RuntimeException("qsort not found"));
            System.out.println("✓ qsort function found");

            // Step 4: Describe the qsort function signature
            FunctionDescriptor qsortDescriptor = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,    // void *base - pointer to array
                    ValueLayout.JAVA_LONG,  // size_t nel - number of elements
                    ValueLayout.JAVA_LONG,  // size_t width - size of each element in bytes
                    ValueLayout.ADDRESS     // comparator function pointer (callback)
            );

            // Step 5: Create a method handle for qsort (downcall)
            MethodHandle qsort = linker.downcallHandle(qsortAddress, qsortDescriptor);
            System.out.println("✓ qsort method handle created");

            // Step 6: Create an upcall stub for our Java comparator
            // This allows C code to call our Java method
            System.out.println("\n--- Setting up callback ---");

            // Get a reference to our Java comparator method
            MethodHandle compareHandle = MethodHandles.lookup()
                    .findStatic(
                            QSortCallbackExample.class,
                            "compareIntegers",
                            MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class)
                    );
            System.out.println("✓ Java comparator method handle created");

            // Describe the comparator function signature for native code
            // C signature: int (*compar)(const void *, const void *)
            FunctionDescriptor comparatorDescriptor = FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,  // return type: int
                    ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT),  // const void *a
                    ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT)   // const void *b
            );

            // Create the actual upcall stub (native code that calls Java)
            // We use a confined arena so the callback is cleaned up automatically
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment comparatorStub = linker.upcallStub(
                        compareHandle,
                        comparatorDescriptor,
                        arena
                );
                System.out.println("✓ Upcall stub created (native->Java bridge)");

                // Step 7: Prepare data to sort
                System.out.println("\n--- Sorting demonstration ---");
                int[] unsortedArray = {42, 7, 23, 91, 5, 68, 13, 99, 1, 56};
                System.out.println("Original array: " + Arrays.toString(unsortedArray));

                // Allocate native memory for the array
                MemorySegment nativeArray = arena.allocateArray(
                        ValueLayout.JAVA_INT,
                        unsortedArray
                );
                System.out.println("✓ Array copied to native memory");

                // Step 8: Call qsort!
                // The C qsort function will call our Java comparator multiple times
                System.out.println("\nCalling qsort() - watch as C calls our Java comparator...");
                qsort.invoke(
                        nativeArray,                                    // pointer to array
                        (long) unsortedArray.length,                   // number of elements
                        (long) ValueLayout.JAVA_INT.byteSize(),        // size of each element (4 bytes)
                        comparatorStub                                  // our Java callback
                );

                // Step 9: Read the sorted data back from native memory
                int[] sortedArray = nativeArray.toArray(ValueLayout.JAVA_INT);
                System.out.println("\n✓ Sorting complete!");
                System.out.println("Sorted array:   " + Arrays.toString(sortedArray));

                // Verify it's actually sorted
                boolean isSorted = true;
                for (int i = 0; i < sortedArray.length - 1; i++) {
                    if (sortedArray[i] > sortedArray[i + 1]) {
                        isSorted = false;
                        break;
                    }
                }

                System.out.println("\n" + (isSorted ? "✓" : "✗") + " Array is " +
                        (isSorted ? "correctly sorted!" : "NOT sorted!"));

                // Show what happened
                System.out.println("\n=== What Just Happened ===");
                System.out.println("1. We wrote a Java method (compareIntegers)");
                System.out.println("2. We created an 'upcall stub' - a native function that calls our Java method");
                System.out.println("3. We passed this stub to C's qsort() function");
                System.out.println("4. qsort() repeatedly called OUR Java code to compare elements");
                System.out.println("5. The sorting happened in C, but the logic was in Java!");
                System.out.println("\nThis is the power of bidirectional FFM API! 🚀");

            } // Arena closes here, automatically cleaning up native memory and callback

            System.out.println("\n✓ All native memory and callbacks automatically cleaned up");

        } catch (Throwable e) {
            System.err.println("\n✗ Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}