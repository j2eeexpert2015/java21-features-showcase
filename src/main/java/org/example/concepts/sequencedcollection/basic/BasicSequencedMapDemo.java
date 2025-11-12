package org.example.concepts.sequencedcollection.basic;

import java.util.*;

/**
 * Demonstrates SequencedMap interface - ordered maps with key-value pairs.
 *
 * Key Concepts:
 * - SequencedMap extends Map<K,V> (NOT SequencedCollection - maps are not collections)
 * - Works with Map.Entry<K,V> (key-value pairs), not individual elements
 * - Different method names: firstEntry/lastEntry (not getFirst/getLast)
 * - Provides sequenced views: sequencedKeySet(), sequencedValues(), sequencedEntrySet()
 *
 * Implementations:
 * - LinkedHashMap: maintains insertion order
 * - TreeMap: maintains key-sorted order (via SortedMap/NavigableMap)
 */
public class BasicSequencedMapDemo {

    public static void main(String[] args) {
        System.out.println("=== BasicSequencedMapDemo ===");

        demonstrateEntryOperations();
        demonstratePutFirstPutLast();
        demonstrateSequencedViews();
        demonstrateInsertionVsSortedOrder();

        System.out.println();
    }

    /**
     * Demonstrates firstEntry/lastEntry operations which return Map.Entry<K,V>
     * (not individual keys or values).
     *
     * Real-world scenario: Task queue with priority levels
     * Processing tasks in the order they were added, with ability to
     * inspect and remove from both ends of the queue.
     */
    private static void demonstrateEntryOperations() {
        System.out.println("\n1. Entry Operations - Working with Key-Value Pairs:");
        System.out.println("Scenario: Task Queue Processing\n");

        SequencedMap<String, String> taskQueue = new LinkedHashMap<>();
        taskQueue.put("TASK-001", "Process payment");
        taskQueue.put("TASK-002", "Send confirmation email");
        taskQueue.put("TASK-003", "Update inventory");

        System.out.println("Task queue: " + taskQueue);

        // Get first and last entries (inspect without removing)
        Map.Entry<String, String> firstTask = taskQueue.firstEntry();
        Map.Entry<String, String> lastTask = taskQueue.lastEntry();

        System.out.println("Next task to process: " + firstTask.getKey() + " -> " + firstTask.getValue());
        System.out.println("Last task in queue: " + lastTask.getKey() + " -> " + lastTask.getValue());

        // pollFirstEntry - removes and returns first entry (FIFO processing)
        Map.Entry<String, String> processedTask = taskQueue.pollFirstEntry();
        System.out.println("\nProcessed task: " + processedTask.getKey() + " -> " + processedTask.getValue());
        System.out.println("Remaining tasks: " + taskQueue);

        // pollLastEntry - removes and returns last entry (cancel last added task)
        Map.Entry<String, String> cancelledTask = taskQueue.pollLastEntry();
        System.out.println("\nCancelled last task: " + cancelledTask.getKey() + " -> " + cancelledTask.getValue());
        System.out.println("Remaining tasks: " + taskQueue);
    }

    /**
     * Demonstrates putFirst() and putLast() for adding entries at specific positions.
     * These control where entries appear in the iteration order.
     *
     * Real-world scenario: Session event tracking with priority events
     * Regular events go to the end, but critical security events jump to the front.
     */
    private static void demonstratePutFirstPutLast() {
        System.out.println("\n2. putFirst() and putLast() - Controlling Order:");
        System.out.println("Scenario: Session Event Tracking\n");

        SequencedMap<String, String> sessionEvents = new LinkedHashMap<>();

        // Regular events - added to end (normal flow)
        sessionEvents.put("09:00:01", "user-login");
        sessionEvents.put("09:00:15", "page-view");
        System.out.println("After regular events: " + sessionEvents);

        // putLast - explicitly adds to end (like regular put, but intention is clear)
        sessionEvents.putLast("09:00:45", "add-to-cart");
        System.out.println("After putLast('09:00:45', 'add-to-cart'): " + sessionEvents);

        // putFirst - adds to beginning (critical security event needs immediate attention)
        sessionEvents.putFirst("09:00:00", "security-check-passed");
        System.out.println("After putFirst('09:00:00', 'security-check-passed'): " + sessionEvents);

        // putFirst with existing key - updates value AND moves to first position
        sessionEvents.putFirst("09:00:15", "page-view-suspicious");
        System.out.println("After putFirst('09:00:15', 'page-view-suspicious'): " + sessionEvents);
        System.out.println("→ Event updated and moved to first position for review!");
    }

    /**
     * Demonstrates sequenced views: sequencedKeySet(), sequencedValues(), sequencedEntrySet()
     * These return SequencedSet/SequencedCollection that support all sequencing operations.
     */
    private static void demonstrateSequencedViews() {
        System.out.println("\n3. Sequenced Views - Keys, Values, Entries:");

        SequencedMap<Integer, String> rankings = new LinkedHashMap<>();
        rankings.put(1, "Gold");
        rankings.put(2, "Silver");
        rankings.put(3, "Bronze");

        System.out.println("Original map: " + rankings);

        // sequencedKeySet() returns SequencedSet<K>
        SequencedSet<Integer> keys = rankings.sequencedKeySet();
        System.out.println("\nKeys (as SequencedSet): " + keys);
        System.out.println("First key: " + keys.getFirst());
        System.out.println("Last key: " + keys.getLast());
        System.out.println("Reversed keys: " + keys.reversed());

        // sequencedValues() returns SequencedCollection<V>
        SequencedCollection<String> values = rankings.sequencedValues();
        System.out.println("\nValues (as SequencedCollection): " + values);
        System.out.println("First value: " + values.getFirst());
        System.out.println("Last value: " + values.getLast());
        System.out.println("Reversed values: " + values.reversed());

        // sequencedEntrySet() returns SequencedSet<Map.Entry<K,V>>
        SequencedSet<Map.Entry<Integer, String>> entries = rankings.sequencedEntrySet();
        System.out.println("\nEntries (as SequencedSet):");
        System.out.println("First entry: " + entries.getFirst());
        System.out.println("Last entry: " + entries.getLast());

        // All views are backed by the map - changes reflect in original
        System.out.println("\n→ All views support reversed() and sequenced operations!");
    }

    /**
     * Demonstrates the difference between insertion-ordered maps (LinkedHashMap)
     * and key-sorted maps (TreeMap). Both implement SequencedMap but have different
     * ordering strategies.
     *
     * Real-world scenario: Request tracking and error monitoring
     * - LinkedHashMap: Track requests in the order they arrived (timestamp order)
     * - TreeMap: Display errors sorted by error code for debugging
     */
    private static void demonstrateInsertionVsSortedOrder() {
        System.out.println("\n4. Insertion Order vs Sorted Order:");
        System.out.println("Scenario: API Request & Error Monitoring\n");

        // LinkedHashMap - maintains insertion order (arrival time)
        System.out.println("a) LinkedHashMap (Insertion Order - Request Timeline):");
        SequencedMap<String, String> requestLog = new LinkedHashMap<>();
        requestLog.put("POST /api/users", "201 Created");
        requestLog.put("GET /api/products", "200 OK");
        requestLog.put("DELETE /api/orders/123", "204 No Content");
        requestLog.put("GET /api/users", "200 OK");

        System.out.println("Requests in arrival order: " + requestLog);
        System.out.println("First request: " + requestLog.firstEntry() + " (earliest)");
        System.out.println("Last request: " + requestLog.lastEntry() + " (most recent)");

        // TreeMap - maintains key-sorted order (error codes sorted)
        System.out.println("\nb) TreeMap (Key-Sorted Order - Error Code Analysis):");
        SequencedMap<String, String> errorCodes = new TreeMap<>();
        errorCodes.put("500", "Internal Server Error");
        errorCodes.put("404", "Not Found");
        errorCodes.put("403", "Forbidden");
        errorCodes.put("400", "Bad Request");

        System.out.println("Errors sorted by code: " + errorCodes);
        System.out.println("First error: " + errorCodes.firstEntry() + " (lowest code)");
        System.out.println("Last error: " + errorCodes.lastEntry() + " (highest code)");

        // Both support reversed() with same API
        System.out.println("\nc) Reversed Maps:");
        SequencedMap<String, String> reversedRequests = requestLog.reversed();
        SequencedMap<String, String> reversedErrors = errorCodes.reversed();

        System.out.println("Requests reversed (newest first): " + reversedRequests);
        System.out.println("Errors reversed (highest to lowest): " + reversedErrors);

        // Demonstrate consistent API
        System.out.println("\nd) Consistent API Across Both:");
        System.out.println("Request log - first key: " + requestLog.sequencedKeySet().getFirst());
        System.out.println("Error codes - first key: " + errorCodes.sequencedKeySet().getFirst());
        System.out.println("→ Same method, different ordering strategy!");
    }
}