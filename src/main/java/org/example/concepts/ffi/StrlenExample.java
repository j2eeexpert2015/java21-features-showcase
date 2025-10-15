package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Demonstrates calling C's strlen() function using Java's Foreign Function & Memory API.
 *
 * The FFM API is in PREVIEW in Java 21 (JEP 442 - Third Preview)
 * It was finalized in Java 22 (JEP 454)
 *
 * Requirements: Java 21 with --enable-preview flag
 *
 * Compile: javac --enable-preview --release 21 StrlenExample.java
 * Run: java --enable-preview org.example.concepts.foreignfunctionmemory.StrlenExample
 *
 * @author Your Name
 */
@SuppressWarnings("preview")  // Suppresses preview API warnings
public class StrlenExample {

    public static void main(String[] args) {
        System.out.println("=== Foreign Function & Memory API Demo (Java 21 Preview) ===");
        System.out.println("Java Version: " + Runtime.version());
        System.out.println("Calling C's strlen() function from Java\n");

        try {
            // Step 1: Get the native linker
            Linker linker = Linker.nativeLinker();
            System.out.println("✓ Native linker obtained");

            // Step 2: Load the C standard library (available on all platforms)
            SymbolLookup stdlib = linker.defaultLookup();
            System.out.println("✓ C standard library loaded");

            // Step 3: Find the strlen function
            MemorySegment strlenAddress = stdlib.find("strlen")
                    .orElseThrow(() -> new RuntimeException("strlen not found in C library"));
            System.out.println("✓ strlen function found");

            // Step 4: Describe the function signature
            // C signature: size_t strlen(const char *str)
            // - Returns: size_t (mapped to JAVA_LONG)
            // - Parameter: const char* (mapped to ADDRESS)
            FunctionDescriptor strlenDescriptor = FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,      // return type: size_t
                    ValueLayout.ADDRESS         // parameter: const char*
            );
            System.out.println("✓ Function descriptor created");

            // Step 5: Create a method handle (downcall from Java to native)
            MethodHandle strlen = linker.downcallHandle(
                    strlenAddress,
                    strlenDescriptor
            );
            System.out.println("✓ Method handle created\n");

            // Step 6: Use it with different strings
            testStrlen(strlen, "Hello, World!");
            testStrlen(strlen, "Foreign Function & Memory API");
            testStrlen(strlen, "Java 21");
            testStrlen(strlen, "");
            testStrlen(strlen, "🚀 Unicode support! 🎉");

        } catch (Throwable e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to test strlen with a given string
     */
    private static void testStrlen(MethodHandle strlen, String text) throws Throwable {
        // Create an arena for memory management
        // Arena ensures automatic cleanup when closed
        try (Arena arena = Arena.ofConfined()) {
            // Allocate native memory and copy our string (null-terminated)
            MemorySegment nativeString = arena.allocateUtf8String(text);

            // Call the native strlen function
            long length = (long) strlen.invoke(nativeString);

            // Display results
            System.out.println("String: \"" + text + "\"");
            System.out.println("  └─ C strlen():    " + length + " bytes");
            System.out.println("  └─ Java length(): " + text.length() + " characters");

            // Note: strlen counts bytes, Java length() counts characters
            // For ASCII they're the same, but Unicode characters may differ
            if (length != text.length()) {
                System.out.println("  └─ Note: Difference due to UTF-8 encoding");
            }
            System.out.println();

        } // Memory automatically freed here!
    }
}