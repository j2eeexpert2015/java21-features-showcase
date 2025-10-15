package org.example.concepts.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Demo : Basic Downcall - Calling C functions from Java
 *
 * Shows how to call C's strlen() using the Foreign Function & Memory API.
 * This is called a "downcall" - Java calling into native code.
 *
 * Requirements: Java 21+ with --enable-preview flag
 */
@SuppressWarnings("preview")
public class BasicDowncall {

    public static void main(String[] args) {
        System.out.println("=== Demo 1: Basic Downcall ===");
        System.out.println("Calling C's strlen() from Java\n");

        try {
            // Step 1: Get the native linker
            Linker linker = Linker.nativeLinker();

            // Step 2: Lookup the C standard library
            SymbolLookup stdlib = linker.defaultLookup();

            // Step 3: Find the strlen function
            MemorySegment strlenAddr = stdlib.find("strlen")
                    .orElseThrow(() -> new RuntimeException("strlen not found"));

            // Step 4: Define the function signature
            // C: size_t strlen(const char *str)
            FunctionDescriptor descriptor = FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,    // return: size_t
                    ValueLayout.ADDRESS       // param: const char*
            );

            // Step 5: Create a method handle (Java -> Native bridge)
            MethodHandle strlen = linker.downcallHandle(strlenAddr, descriptor);

            // Step 6: Use it!
            testStrlen(strlen, "Hello, World!");
            testStrlen(strlen, "Java 21 FFM API");
            testStrlen(strlen, "🚀 Unicode!");

        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testStrlen(MethodHandle strlen, String text) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate native memory for the string
            MemorySegment nativeStr = arena.allocateUtf8String(text);

            // Call native strlen
            long length = (long) strlen.invoke(nativeStr);

            System.out.printf("\"%s\"%n", text);
            System.out.printf("  C strlen():    %d bytes%n", length);
            System.out.printf("  Java length(): %d chars%n", text.length());
            if (length != text.length()) {
                System.out.println("  (UTF-8 encoding uses multiple bytes for some characters)");
            }
            System.out.println();
        }
    }
}