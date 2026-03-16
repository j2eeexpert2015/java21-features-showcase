package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/*
 * Demo 1: Downcall — Java calling C's strlen() using the FFM API
 * No JNI, no C code, no native compiler required
 */
@SuppressWarnings("preview")
public class BasicDowncall {

    public static void main(String[] args) {
        System.out.println("=== Demo 1: Basic Downcall ===");
        System.out.println("Calling C's strlen() from Java\n");

        try {
            // Step 1: Get the native linker — entry point to native code
            Linker linker = Linker.nativeLinker();

            // Step 2: Get a lookup for the C standard library
            SymbolLookup stdlib = linker.defaultLookup();

            // Step 3: Find strlen's address in memory
            MemorySegment strlenAddr = stdlib.find("strlen")
                    .orElseThrow(() -> new RuntimeException("strlen not found"));

            /*
             * Step 4: Describe the function signature
             * C signature: size_t strlen(const char *str)
             */
            FunctionDescriptor descriptor = FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,  // return: size_t
                    ValueLayout.ADDRESS     // param:  const char*
            );

            // Step 5: Create a callable method handle
            MethodHandle strlen = linker.downcallHandle(strlenAddr, descriptor);

            // Step 6: Test with sample strings
            testStrlen(strlen, "Hello, World!");
            testStrlen(strlen, "Java 21 FFM API");
            testStrlen(strlen, "🚀 Unicode!");

        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /*
     * Allocates a native string, calls C strlen,
     * prints byte vs char comparison
     */
    private static void testStrlen(MethodHandle strlen, String text) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeStr = arena.allocateUtf8String(text);
            long length = (long) strlen.invoke(nativeStr);

            System.out.println("Sample text   : " + text);
            System.out.printf( "C strlen()    : %d bytes%n", length);
            System.out.printf( "Java length() : %d chars%n", text.length());
            if (length != text.length()) {
                System.out.println("Note          : UTF-8 uses multiple bytes for this character");
            }
            System.out.println();
        }
    }
}