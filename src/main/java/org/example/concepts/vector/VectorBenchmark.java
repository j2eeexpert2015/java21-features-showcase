package org.example.concepts.vector;

import jdk.incubator.vector.*;
import java.util.Random;

public class VectorBenchmark {
    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        // --- Env info
        System.out.println("Java: " + System.getProperty("java.runtime.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
        System.out.println("Cores: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Vector bits (int): " + SPECIES.vectorBitSize() + ", lanes: " + SPECIES.length());

        final int size = 100_000_000; // try larger too: 10_000_000 or 50_000_000
        int[] a = new int[size], b = new int[size];
        int[] outScalar = new int[size], outVector = new int[size];

        Random r = new Random(42);
        for (int i = 0; i < size; i++) {
            a[i] = r.nextInt(); b[i] = r.nextInt();
        }

        // warm-up
        for (int k = 0; k < 5; k++) { scalar(a,b,outScalar); vector(a,b,outVector); }

        // timed runs (take best of N to reduce noise)
        long bestScalar = Long.MAX_VALUE, bestVector = Long.MAX_VALUE;
        long cs1 = 0, cs2 = 0;
        for (int k = 0; k < 10; k++) {
            long t0 = System.nanoTime();
            scalar(a,b,outScalar);
            long t1 = System.nanoTime();
            vector(a,b,outVector);
            long t2 = System.nanoTime();
            bestScalar = Math.min(bestScalar, t1 - t0);
            bestVector = Math.min(bestVector, t2 - t1);
            cs1 ^= checksum(outScalar);
            cs2 ^= checksum(outVector);
        }

        System.out.println("Scalar best: " + bestScalar/1_000_000.0 + " ms");
        System.out.println("Vector best: " + bestVector/1_000_000.0 + " ms");
        System.out.println("Checksums (avoid DCE): " + cs1 + " / " + cs2);
    }

    static void scalar(int[] a, int[] b, int[] out) {
        for (int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
    }

    static void vector(int[] a, int[] b, int[] out) {
        int i = 0, upper = SPECIES.loopBound(a.length);
        // main loop: NO MASK (fast contiguous loads/stores)
        for (; i < upper; i += SPECIES.length()) {
            IntVector va = IntVector.fromArray(SPECIES, a, i);
            IntVector vb = IntVector.fromArray(SPECIES, b, i);
            va.add(vb).intoArray(out, i);
        }
        // tail only
        if (i < a.length) {
            VectorMask<Integer> m = SPECIES.indexInRange(i, a.length);
            IntVector va = IntVector.fromArray(SPECIES, a, i, m);
            IntVector vb = IntVector.fromArray(SPECIES, b, i, m);
            va.add(vb).intoArray(out, i, m);
        }
    }

    static long checksum(int[] x) {
        long s = 0;
        for (int v : x) s += v;
        return s;
    }
}
