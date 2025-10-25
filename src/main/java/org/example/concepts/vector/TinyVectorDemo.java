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
  often looks only slightly faster. To make SIMD’s ALUs matter, we increase the
  amount of arithmetic per element using a small inner loop controlled by R.
  This pushes the benchmark toward COMPUTE-BOUND, where SIMD shines.

  What is R?
  ----------
  R is the "repeat count" (arithmetic intensity knob). Each element performs the
  same core arithmetic R times. Higher R = more work per element = clearer SIMD gain.
  - Keep R moderate (32–256) so the run is quick but still compute-bound.
  - Both scalar and vector paths do identical work; only the way it’s expressed differs.

  How to read the output:
  -----------------------
  - “Scalar” and “Vector” are the best (lowest) times out of a few short runs.
  - “Speedup” = Scalar time / Vector time.
  - GB/s is an *effective* bandwidth number ≈ (read a + read b + write out).
    It’s useful to see when you’re memory-bound (both versions close in GB/s).
*/

public class TinyVectorDemo {
    // Preferred SIMD shape for this CPU/JVM (e.g., 512 bits = 16 lanes of 32-bit ints).
    static final VectorSpecies<Integer> S = IntVector.SPECIES_PREFERRED;

    // Arithmetic intensity knob: increase to amplify SIMD gains (compute-bound),
    // decrease to approach memory-bound behavior.
    static final int R = 128;

    public static void main(String[] args) {
        final int n = 10_000_000; // 10M elements keeps the run short but representative

        int[] a = new int[n], b = new int[n];
        int[] outS = new int[n], outV = new int[n];

        // Fill with deterministic data
        var rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt();
            b[i] = rnd.nextInt();
        }

        // Quick warmup so the JIT has a chance to optimize
        scalar(a, b, outS);
        vector(a, b, outV);
        scalar(a, b, outS);
        vector(a, b, outV);

        // Measure best-of-5 to reduce run-to-run noise
        long bestS = Long.MAX_VALUE, bestV = Long.MAX_VALUE;
        for (int r = 0; r < 5; r++) {
            long t0 = System.nanoTime();
            scalar(a, b, outS);
            long t1 = System.nanoTime();
            vector(a, b, outV);
            long t2 = System.nanoTime();

            bestS = Math.min(bestS, t1 - t0);
            bestV = Math.min(bestV, t2 - t1);
        }

        // Simple DCE guard: sum each output once
        long sumS = 0, sumV = 0;
        for (int v : outS) sumS += v;
        for (int v : outV) sumV += v;

        // Report
        System.out.println("Vector bits: " + S.vectorBitSize() + ", lanes(int): " + S.length());
        System.out.println("Elements  : " + String.format("%,d", n));
        System.out.println("R (repeats per element): " + R);

        double sMs = bestS / 1e6, vMs = bestV / 1e6;
        System.out.printf("Scalar: %.3f ms%n", sMs);
        System.out.printf("Vector: %.3f ms%n", vMs);
        System.out.println("Checksums: " + sumS + " / " + sumV);

        double speedup = (double) bestS / (double) bestV;
        System.out.printf("Speedup (scalar/vector): %.2fx%n", speedup);

        // Effective bandwidth (approx): read a + read b + write out = 12 bytes per element
        double bytes = 12.0 * n;
        double sGBs = (bytes / (bestS / 1e9)) / 1e9;
        double vGBs = (bytes / (bestV / 1e9)) / 1e9;
        System.out.printf("Effective BW: Scalar %.2f GB/s | Vector %.2f GB/s%n", sGBs, vGBs);
    }

    // --------------------------------------------------------------------
    // Scalar version: out[i] accumulates (a[i] * 3 + b[i]) exactly R times.
    // This adds enough ALU work per element to expose SIMD benefits clearly.
    // --------------------------------------------------------------------
    static void scalar(int[] a, int[] b, int[] out) {
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
    static void vector(int[] a, int[] b, int[] out) {
        final var c3 = IntVector.broadcast(S, 3);

        int i = 0, up = S.loopBound(a.length); // up is the largest index < length aligned to a full vector
        for (; i < up; i += S.length()) {
            var va = IntVector.fromArray(S, a, i);
            var vb = IntVector.fromArray(S, b, i);

            // Repeat the same per-element computation R times, but vectorized.
            var acc = IntVector.zero(S);
            for (int r = 0; r < R; r++) {
                acc = acc.add(va.mul(c3).add(vb));
            }
            acc.intoArray(out, i);
        }

        // Tail (if n is not a multiple of the vector length)
        if (i < a.length) {
            var m  = S.indexInRange(i, a.length);
            var va = IntVector.fromArray(S, a, i, m);
            var vb = IntVector.fromArray(S, b, i, m);

            var acc = IntVector.zero(S);
            for (int r = 0; r < R; r++) {
                acc = acc.add(va.mul(c3).add(vb));
            }
            acc.intoArray(out, i, m);
        }
    }
}
