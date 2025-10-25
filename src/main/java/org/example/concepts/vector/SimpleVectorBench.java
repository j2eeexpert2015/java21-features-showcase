package org.example.concepts.vector;

import jdk.incubator.vector.*;
import java.util.Random;

public class SimpleVectorBench {
    static final VectorSpecies<Integer> S = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        final int size = (args.length > 0) ? Integer.parseInt(args[0]) : 10_000_000;
        final String kernel = (args.length > 1) ? args[1].toLowerCase() : "fma"; // default: fma

        System.out.println("Java: " + System.getProperty("java.runtime.version"));
        System.out.println("OS  : " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
        System.out.println("Cores: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Vector species (int): " + S.vectorBitSize() + " bits, lanes=" + S.length());
        System.out.println("Size: " + String.format("%,d", size) + "  | Kernel: " + kernel);
        System.out.println();

        int[] a = new int[size], b = new int[size];
        int[] outScalar = new int[size], outVector = new int[size];

        var rnd = new Random(42);
        for (int i = 0; i < size; i++) {
            a[i] = rnd.nextInt();
            b[i] = rnd.nextInt();
        }

        // Warm-up
        for (int i = 0; i < 5; i++) {
            if (kernel.equals("add")) { scalarAdd(a,b,outScalar); vectorAdd(a,b,outVector); }
            else                      { scalarFma(a,b,outScalar); vectorFma(a,b,outVector); }
        }

        long bestScalar = Long.MAX_VALUE, bestVector = Long.MAX_VALUE;
        for (int r = 0; r < 8; r++) {
            long t0 = System.nanoTime();
            if (kernel.equals("add")) scalarAdd(a,b,outScalar);
            else                      scalarFma(a,b,outScalar);
            long t1 = System.nanoTime();
            if (kernel.equals("add")) vectorAdd(a,b,outVector);
            else                      vectorFma(a,b,outVector);
            long t2 = System.nanoTime();

            bestScalar = Math.min(bestScalar, t1 - t0);
            bestVector = Math.min(bestVector, t2 - t1);
        }

        long sumS = 0, sumV = 0;
        for (int v : outScalar) sumS += v;
        for (int v : outVector) sumV += v;

        double bytes = 12.0 * size;
        double sMs = bestScalar / 1e6, vMs = bestVector / 1e6;
        double sGBs = (bytes / (bestScalar / 1e9)) / 1e9;
        double vGBs = (bytes / (bestVector / 1e9)) / 1e9;

        System.out.printf("Scalar best: %.3f ms  (%.2f GB/s)%n", sMs, sGBs);
        System.out.printf("Vector best: %.3f ms  (%.2f GB/s)%n", vMs, vGBs);
        System.out.println("Checksums  : " + sumS + " / " + sumV);
    }

    // ------------------ Scalar ------------------
    static void scalarAdd(int[] a, int[] b, int[] out) {
        for (int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
    }

    static void scalarFma(int[] a, int[] b, int[] out) {
        final int C = 3, M = 0x5a5a5a5a;
        for (int i = 0; i < a.length; i++) out[i] = a[i] * C + (b[i] ^ M);
    }

    // ------------------ Vector ------------------
    static void vectorAdd(int[] a, int[] b, int[] out) {
        int i = 0, upper = S.loopBound(a.length);
        for (; i < upper; i += S.length()) {
            var va = IntVector.fromArray(S, a, i);
            var vb = IntVector.fromArray(S, b, i);
            va.add(vb).intoArray(out, i);
        }
        if (i < a.length) {
            var m = S.indexInRange(i, a.length);
            var va = IntVector.fromArray(S, a, i, m);
            var vb = IntVector.fromArray(S, b, i, m);
            va.add(vb).intoArray(out, i, m);
        }
    }

    static void vectorFma(int[] a, int[] b, int[] out) {
        final var c3 = IntVector.broadcast(S, 3);
        final var mask = IntVector.broadcast(S, 0x5a5a5a5a);
        int i = 0, upper = S.loopBound(a.length);
        for (; i < upper; i += S.length()) {
            var va = IntVector.fromArray(S, a, i);
            var vb = IntVector.fromArray(S, b, i);
            // xor replaced with lanewise()
            var vx = vb.lanewise(VectorOperators.XOR, mask);
            va.mul(c3).add(vx).intoArray(out, i);
        }
        if (i < a.length) {
            var m = S.indexInRange(i, a.length);
            var va = IntVector.fromArray(S, a, i, m);
            var vb = IntVector.fromArray(S, b, i, m);
            var vx = vb.lanewise(VectorOperators.XOR, mask, m);
            va.mul(c3).add(vx, m).intoArray(out, i, m);
        }
    }
}
