package org.example.concepts.vector;

import jdk.incubator.vector.*;
import java.util.Random;

/**
 * DualScenarioVectorDemo
 * ----------------------
 * Demonstrates two scenarios showing when the Java Vector API helps slightly
 * (memory-bound) and when it helps significantly (CPU-bound).
 *
 * Scenario A: MEMORY-BOUND
 *     Performs a simple element-wise addition: out[i] = a[i] + b[i]
 *     Work per element is minimal. Performance is limited by memory bandwidth.
 *     Vector API may only provide a small improvement or none.
 *
 * Scenario B: CPU-BOUND
 *     Performs heavier arithmetic per element: acc += a[i] * 3 + b[i], repeated R times.
 *     Increasing R adds more compute work per element. SIMD vectorization here
 *     can provide a noticeable performance improvement.
 *
 * Explanation of key concepts:
 *     VectorSpecies – Describes the SIMD shape supported by the CPU (e.g., 512 bits → 16 lanes of int).
 *     R – The repeat count that increases arithmetic work per element in the CPU-bound case.
 *     Effective Bandwidth (GB/s) – Estimated throughput of memory operations.
 *        Approximation = (read a + read b + write out) / time.
 *        Useful to identify if a workload is limited by memory or compute.
 *     Checksum – The sum of all output values, used to ensure computations are
 *        not optimized away by the JVM (prevents Dead Code Elimination).
 *
 * Tip:
 *     Run once with "-XX:-UseSuperWord" to disable JVM auto-vectorization
 *     and get a true scalar baseline comparison.
 */
public class DualScenarioVectorDemo {

    /** Preferred SIMD shape for the CPU (for example, 512 bits = 16 x int lanes). */
    static final VectorSpecies<Integer> S = IntVector.SPECIES_PREFERRED;

    /** Controls arithmetic intensity for the CPU-bound case. Higher R = more work per element. */
    static final int R = 128;

    public static void main(String[] args) {
        final int n = 10_000_000; // 10 million elements
        final long seed = 42;

        int[] a = new int[n], b = new int[n];
        int[] outScalar = new int[n], outVector = new int[n];

        Random rnd = new Random(seed);
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt();
            b[i] = rnd.nextInt();
        }

        System.out.println("=== Vector Capability ===");
        System.out.println("Vector bits (int): " + S.vectorBitSize() + ", lanes: " + S.length());
        System.out.println("Array size       : " + String.format("%,d", n));
        System.out.println();

        // ============================
        // Scenario A: MEMORY-BOUND
        // ============================
        System.out.println("=== Scenario A: MEMORY-BOUND (out[i] = a[i] + b[i]) ===");
        warmup(() -> scalarMemoryBound(a, b, outScalar), () -> vectorMemoryBound(a, b, outVector));

        long scalarA = bestOf(5, () -> scalarMemoryBound(a, b, outScalar));
        long vectorA = bestOf(5, () -> vectorMemoryBound(a, b, outVector));

        long sumScalarA = checksum(outScalar);
        long sumVectorA = checksum(outVector);

        report(n, scalarA, vectorA, "Scenario A (Memory-Bound)", sumScalarA, sumVectorA);

        // ============================
        // Scenario B: CPU-BOUND
        // ============================
        System.out.println("\n=== Scenario B: CPU-BOUND (repeat R times: acc += a*3 + b) ===");
        System.out.println("R (repeats per element): " + R);
        warmup(() -> scalarCpuBound(a, b, outScalar), () -> vectorCpuBound(a, b, outVector));

        long scalarB = bestOf(5, () -> scalarCpuBound(a, b, outScalar));
        long vectorB = bestOf(5, () -> vectorCpuBound(a, b, outVector));

        long sumScalarB = checksum(outScalar);
        long sumVectorB = checksum(outVector);

        report(n, scalarB, vectorB, "Scenario B (CPU-Bound)", sumScalarB, sumVectorB);

        System.out.println("\nTip: Run once with -XX:-UseSuperWord to disable JVM auto-vectorization");
        System.out.println("     That shows a clearer difference between scalar and vector performance.");
    }

    /** Performs simple element-wise addition (memory-bound scenario). */
    static void scalarMemoryBound(int[] a, int[] b, int[] out) {
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i] + b[i];
        }
    }

    /** Vectorized version of element-wise addition using the Vector API. */
    static void vectorMemoryBound(int[] a, int[] b, int[] out) {
        int i = 0, upper = S.loopBound(a.length);
        for (; i < upper; i += S.length()) {
            IntVector va = IntVector.fromArray(S, a, i);
            IntVector vb = IntVector.fromArray(S, b, i);
            va.add(vb).intoArray(out, i);
        }
        if (i < a.length) {
            VectorMask<Integer> mask = S.indexInRange(i, a.length);
            IntVector va = IntVector.fromArray(S, a, i, mask);
            IntVector vb = IntVector.fromArray(S, b, i, mask);
            va.add(vb).intoArray(out, i, mask);
        }
    }

    /** Performs a compute-heavy operation (CPU-bound scenario) in scalar form. */
    static void scalarCpuBound(int[] a, int[] b, int[] out) {
        final int C = 3;
        for (int i = 0; i < a.length; i++) {
            int acc = 0;
            int x = a[i], y = b[i];
            for (int r = 0; r < R; r++) {
                acc += x * C + y;
            }
            out[i] = acc;
        }
    }

    /** Vectorized version of the same compute-heavy operation using the Vector API. */
    static void vectorCpuBound(int[] a, int[] b, int[] out) {
        final IntVector c3 = IntVector.broadcast(S, 3);
        int i = 0, upper = S.loopBound(a.length);
        for (; i < upper; i += S.length()) {
            IntVector vx = IntVector.fromArray(S, a, i);
            IntVector vy = IntVector.fromArray(S, b, i);
            IntVector acc = IntVector.zero(S);
            for (int r = 0; r < R; r++) {
                acc = acc.add(vx.mul(c3).add(vy));
            }
            acc.intoArray(out, i);
        }
        if (i < a.length) {
            VectorMask<Integer> mask = S.indexInRange(i, a.length);
            IntVector vx = IntVector.fromArray(S, a, i, mask);
            IntVector vy = IntVector.fromArray(S, b, i, mask);
            IntVector acc = IntVector.zero(S);
            for (int r = 0; r < R; r++) {
                acc = acc.add(vx.mul(c3).add(vy));
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
     * Prints results including scalar and vector execution times, speedup,
     * effective bandwidth, and checksum values for verification.
     * Effective Bandwidth ≈ 12 bytes per element (read a + read b + write out).
     */
    static void report(int n, long nsScalar, long nsVector, String label, long sumS, long sumV) {
        double scalarMs = nsScalar / 1e6;
        double vectorMs = nsVector / 1e6;
        double speedup = (double) nsScalar / (double) nsVector;

        double bytes = 12.0 * n;
        double scalarGBs = (bytes / (nsScalar / 1e9)) / 1e9;
        double vectorGBs = (bytes / (nsVector / 1e9)) / 1e9;

        System.out.println("\n--- " + label + " ---");
        System.out.printf("Scalar: %.3f ms | Vector: %.3f ms | Speedup: %.2fx%n", scalarMs, vectorMs, speedup);
        System.out.printf("Effective Bandwidth: Scalar %.2f GB/s | Vector %.2f GB/s%n", scalarGBs, vectorGBs);
        System.out.printf("Checksums: %d / %d%n", sumS, sumV);
    }
}
