package org.example.concepts.vector;

import jdk.incubator.vector.*;
import java.util.Random;

/*
  Tiny, self-contained benchmark that contrasts:
    - A plain scalar loop
    - The same loop written with the Vector API (SIMD)

  Why this shows a difference:
  ---------------------------
  Pure array addition is usually MEMORY-BOUND (RAM bandwidth dominates), so SIMD
  often looks only slightly faster. To make SIMD's ALUs matter, we increase the
  amount of arithmetic per element using a small inner loop controlled by R.
  This pushes the benchmark toward COMPUTE-BOUND, where SIMD shines.

  What is R?
  ----------
  R is the "repeat count" (arithmetic intensity knob). Each element performs the
  same core arithmetic R times. Higher R = more work per element = clearer SIMD gain.
  - Keep R moderate (32–256) so the run is quick but still compute-bound.
  - Both scalar and vector paths do identical work; only the way it's expressed differs.

  How to read the output:
  -----------------------
  - "Scalar" and "Vector" are the best (lowest) times out of a few short runs.
  - "Speedup" = Scalar time / Vector time.
  - Checksums are printed side by side — if they match, both paths produced identical results.
*/

public class ScalarVsVectorDemo {

    /** Preferred SIMD shape for this CPU/JVM (e.g., 512 bits = 16 lanes of 32-bit ints). */
    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    /** Arithmetic intensity knob: increase to amplify SIMD gains (compute-bound),
     *  decrease to approach memory-bound behavior. */
    static final int R = 128;

    public static void main(String[] args) {
        final int n = 10_000_000; // 10M elements keeps the run short but representative

        int[] a = new int[n], b = new int[n];
        int[] outScalar = new int[n], outVector = new int[n];

        // Fill with deterministic data
        var rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt();
            b[i] = rnd.nextInt();
        }

        System.out.println("=== Vector Capability ===");
        System.out.println("Vector bits (int): " + SPECIES.vectorBitSize() + ", lanes: " + SPECIES.length());
        System.out.println("Elements         : " + String.format("%,d", n));
        System.out.println("R (repeats per element): " + R);
        System.out.println();

        // Warmup — give the JIT a chance to compile and optimise both methods
        warmup(() -> scalarCompute(a, b, outScalar), () -> vectorCompute(a, b, outVector));

        // Measure best-of-5 to reduce run-to-run noise
        long bestScalar = bestOf(5, () -> scalarCompute(a, b, outScalar));
        long bestVector = bestOf(5, () -> vectorCompute(a, b, outVector));

        // Checksum — confirms correctness and prevents dead-code elimination
        long sumScalar = checksum(outScalar);
        long sumVector = checksum(outVector);

        report(n, bestScalar, bestVector, sumScalar, sumVector);
    }

    // --------------------------------------------------------------------
    // Scalar version: out[i] accumulates (a[i] * 3 + b[i]) exactly R times.
    // This adds enough ALU work per element to expose SIMD benefits clearly.
    // --------------------------------------------------------------------
    static void scalarCompute(int[] a, int[] b, int[] out) {
        final int C = 3;
        for (int i = 0; i < a.length; i++) {
            int x = a[i], y = b[i], acc = 0;
            for (int r = 0; r < R; r++) {
                acc += x * C + y;
            }
            out[i] = acc;
        }
    }

    // --------------------------------------------------------------------
    // Vector version: same math, expressed with the Vector API.
    // We keep the main loop mask-free and use a masked tail only if needed.
    // --------------------------------------------------------------------
    static void vectorCompute(int[] a, int[] b, int[] out) {
        final var c3 = IntVector.broadcast(SPECIES, 3);

        int i = 0, upper = SPECIES.loopBound(a.length);
        for (; i < upper; i += SPECIES.length()) {
            var vecA = IntVector.fromArray(SPECIES, a, i);
            var vecB = IntVector.fromArray(SPECIES, b, i);
            var acc  = IntVector.zero(SPECIES);
            for (int r = 0; r < R; r++) {
                acc = acc.add(vecA.mul(c3).add(vecB));
            }
            acc.intoArray(out, i);
        }

        // Tail — handles remaining elements if n is not a multiple of vector length
        if (i < a.length) {
            var mask = SPECIES.indexInRange(i, a.length);
            var vecA = IntVector.fromArray(SPECIES, a, i, mask);
            var vecB = IntVector.fromArray(SPECIES, b, i, mask);
            var acc  = IntVector.zero(SPECIES);
            for (int r = 0; r < R; r++) {
                acc = acc.add(vecA.mul(c3).add(vecB));
            }
            acc.intoArray(out, i, mask);
        }
    }

    /** Performs a few warm-up runs so the JIT compiler optimizes the code. */
    static void warmup(Runnable scalar, Runnable vector) {
        scalar.run(); vector.run();
        scalar.run(); vector.run();
    }

    /** Executes the runnable multiple times and returns the best (minimum) execution time. */
    static long bestOf(int iterations, Runnable r) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            r.run();
            long end = System.nanoTime();
            best = Math.min(best, end - start);
        }
        return best;
    }

    /** Calculates a simple checksum (sum of all elements) to prevent dead-code elimination. */
    static long checksum(int[] array) {
        long sum = 0;
        for (int v : array) sum += v;
        return sum;
    }

    /**
     * Prints scalar and vector execution times, speedup, and checksum values for verification.
     */
    static void report(int n, long nsScalar, long nsVector, long sumS, long sumV) {
        double scalarMs = nsScalar / 1e6;
        double vectorMs = nsVector / 1e6;
        double speedup  = (double) nsScalar / (double) nsVector;

        System.out.printf("Scalar : %.3f ms%n", scalarMs);
        System.out.printf("Vector : %.3f ms%n", vectorMs);
        System.out.printf("Speedup (scalar/vector): %.2fx%n", speedup);
        System.out.println("Checksums: " + sumS + " / " + sumV);
    }
}