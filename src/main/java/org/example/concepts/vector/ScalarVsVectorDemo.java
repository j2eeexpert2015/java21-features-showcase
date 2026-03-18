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
  - GB/s is an *effective* bandwidth number ≈ (read a + read b + write out).
    It's useful to see when you're memory-bound (both versions close in GB/s).
*/

public class ScalarVsVectorDemo {
    // Preferred SIMD shape for this CPU/JVM (e.g., 512 bits = 16 lanes of 32-bit ints).
    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    // Arithmetic intensity knob: increase to amplify SIMD gains (compute-bound),
    // decrease to approach memory-bound behavior.
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

        // Quick warmup so the JIT has a chance to optimize
        scalarCompute(a, b, outScalar);
        vectorCompute(a, b, outVector);
        scalarCompute(a, b, outScalar);
        vectorCompute(a, b, outVector);

        // Measure best-of-5 to reduce run-to-run noise
        long bestScalar = Long.MAX_VALUE, bestVector = Long.MAX_VALUE;
        for (int r = 0; r < 5; r++) {
            long t0 = System.nanoTime();
            scalarCompute(a, b, outScalar);
            long t1 = System.nanoTime();
            vectorCompute(a, b, outVector);
            long t2 = System.nanoTime();

            bestScalar = Math.min(bestScalar, t1 - t0);
            bestVector = Math.min(bestVector, t2 - t1);
        }

        // Simple DCE guard: sum each output once
        long sumScalar = 0, sumVector = 0;
        for (int v : outScalar) sumScalar += v;
        for (int v : outVector) sumVector += v;

        // Report
        System.out.println("Vector bits: " + SPECIES.vectorBitSize() + ", lanes(int): " + SPECIES.length());
        System.out.println("Elements  : " + String.format("%,d", n));
        System.out.println("R (repeats per element): " + R);

        double scalarMs = bestScalar / 1e6, vectorMs = bestVector / 1e6;
        System.out.printf("Scalar: %.3f ms%n", scalarMs);
        System.out.printf("Vector: %.3f ms%n", vectorMs);
        System.out.println("Checksums: " + sumScalar + " / " + sumVector);

        double speedup = (double) bestScalar / (double) bestVector;
        System.out.printf("Speedup (scalar/vector): %.2fx%n", speedup);

        // Effective bandwidth (approx): read a + read b + write out = 12 bytes per element
        double bytes = 12.0 * n;
        double scalarGBs = (bytes / (bestScalar / 1e9)) / 1e9;
        double vectorGBs = (bytes / (bestVector / 1e9)) / 1e9;
        System.out.printf("Effective BW: Scalar %.2f GB/s | Vector %.2f GB/s%n", scalarGBs, vectorGBs);
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

        // Tail (if n is not a multiple of the vector length)
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
}