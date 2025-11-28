package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * 💣 ZGC Allocation Bomb - Generational ZGC Demo
 *
 * Purpose:
 * This class stresses the Garbage Collector by simulating a realistic but heavy workload.
 * It demonstrates the "Weak Generational Hypothesis" in action:
 * 1. Most objects die young (Young Gen)
 * 2. Few objects survive for a long time (Old Gen)
 *
 * VM Options required to see the magic:
 * -XX:+UseZGC -XX:+ZGenerational -Xmx4G -Xlog:gc*
 */
public class ZGCAllocationBomb {

    /*
     * =============================================================
     * 🐢 LONG-LIVED OBJECTS (OLD GENERATION)
     * =============================================================
     * This list simulates a "Memory Leak" or a massive "Cache".
     * Objects added here NEVER die. They survive every GC cycle.
     * Eventually, they get promoted to the Old Generation.
     */
    private static final List<byte[]> LIVE_SET = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("💣 Starting ZGC Allocation Bomb...");
        System.out.println("   VM Info: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));

        /*
         * -------------------------------------------------------------
         * 🛑 PAUSE 1: CONNECT TOOLS
         * -------------------------------------------------------------
         * We pause here so you can connect VisualVM or JMC *before* any
         * memory is allocated. This lets you record the "Ramp Up" phase.
         */
        waitForEnter("🛑 PAUSE 1: Connect JMC / VisualVM now.\n   Press [ENTER] to start Phase 1 (Filling Old Gen)...");

        /*
         * -------------------------------------------------------------
         * PHASE 1: FILL THE OLD GENERATION
         * -------------------------------------------------------------
         * We create objects and store them in LIVE_SET.
         * The GC sees they are still referenced, so it moves them to Old Gen.
         */
        System.out.println("\n[PHASE 1] Building Long-Lived Objects (Old Gen)...");

        for (int i = 0; i < 1000; i++) {
            // Create a 1MB object and KEEP it (Simulating valid data/cache)
            LIVE_SET.add(new byte[1024 * 1024]);

            if (i % 100 == 0) {
                System.out.println("   ...Cache Size: " + i + "MB (Promoted to Old Gen)");
                Thread.sleep(20); // Sleep to allow GC to promote objects naturally
            }
        }
        System.out.println("✅ Phase 1 Complete. 1GB of Long-Lived Data established.");

        /*
         * -------------------------------------------------------------
         * 🛑 PAUSE 2: VERIFY BASELINE
         * -------------------------------------------------------------
         * Now that Old Gen is full, we pause again.
         * This allows you to show the "Stable" state (Flat Heap Graph)
         * before the chaos begins.
         */
        waitForEnter("🛑 PAUSE 2: Check VisualVM. You should see a stable 1GB Heap.\n   Press [ENTER] to unleash the Short-Lived Object assault...");

        /*
         * -------------------------------------------------------------
         * PHASE 2: THE YOUNG GENERATION ASSAULT
         * -------------------------------------------------------------
         * We create massive amounts of objects that are IMMEDIATELY discarded.
         * This forces Generational ZGC to furiously clean the Young Gen
         * while ignoring our massive Old Gen (LIVE_SET).
         */
        System.out.println("\n[PHASE 2] Starting Short-Lived Object Assault (Allocation Bomb)...");
        System.out.println("   --> Watch 'GC Pause' in JMC (Should be < 1ms)");
        System.out.println("   --> Watch 'Heap Used' (Sawtooth Pattern)");

        long allocationCount = 0;

        while (true) {
            /*
             * 🐇 SHORT-LIVED OBJECTS
             * Allocate 10MB of data instantly and discard it (not added to list).
             */
            for (int i = 0; i < 100; i++) {
                byte[] garbage = new byte[1024 * 100]; // 100KB object -> Becomes Garbage immediately
            }

            allocationCount += 10; // Track roughly how much trash we created

            if (allocationCount % 2000 == 0) {
                System.out.println("   ...Generated " + (allocationCount / 1024) + " GB of Short-Lived Garbage");
            }

            /*
             * Sleep VERY briefly to prevent "Allocation Stall" (CPU Starvation).
             * In a real app, this simulates the time taken to process a request.
             * Try commenting this out to see ZGC struggle!
             */
            Thread.sleep(1);
        }
    }

    /**
     * Helper to pause the app so you can connect profiling tools.
     */
    private static void waitForEnter(String message) {
        System.out.println("--------------------------------------------------");
        System.out.println(message);
        System.out.println("--------------------------------------------------");
        try {
            System.in.read();
            while (System.in.available() > 0) System.in.read(); // Clear buffer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}