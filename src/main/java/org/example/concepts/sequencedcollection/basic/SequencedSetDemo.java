package org.example.concepts.sequencedcollection.basic;

import java.util.*;

/**
 * Demonstrates SequencedSet interface - ordered sets with uniqueness guarantee.
 *
 * Key Concepts:
 * - SequencedSet extends both Set<E> and SequencedCollection<E>
 * - Maintains order (insertion or sorted) while enforcing uniqueness
 * - Methods like addFirst/addLast respect Set semantics (duplicates ignored)
 * - reversed() returns SequencedSet (covariant return type)
 *
 * Implementations:
 * - LinkedHashSet: maintains insertion order
 * - TreeSet: maintains sorted order (via SortedSet/NavigableSet)
 */
public class SequencedSetDemo {

    public static void main(String[] args) {
        System.out.println("=== BasicSequencedSetDemo ===");

        demonstrateSetSemantics();
        demonstrateCovariantReturnType();
        demonstrateInsertionVsSortedOrder();

        System.out.println();
    }

    /**
     * Demonstrates that Set semantics are preserved even when using
     * SequencedCollection methods like addFirst() and addLast().
     * Duplicates are always ignored.
     */
    private static void demonstrateSetSemantics() {
        System.out.println("\n1. Set Semantics - No Duplicates:");

        SequencedSet<String> set = new LinkedHashSet<>();
        set.add("A");
        set.add("B");
        set.add("C");

        System.out.println("Initial set: " + set);

        // Try to add duplicate using regular add()
        set.add("B");
        System.out.println("After set.add('B'): " + set + " (duplicate ignored)");

        // Try to add duplicate using addFirst()
        set.addFirst("A");
        System.out.println("After set.addFirst('A'): " + set + " (duplicate ignored)");

        // Try to add duplicate using addLast()
        set.addLast("C");
        System.out.println("After set.addLast('C'): " + set + " (duplicate ignored)");

        // Add new element with addFirst
        set.addFirst("Z");
        System.out.println("After set.addFirst('Z'): " + set + " (new element added at start)");
    }

    /**
     * Demonstrates covariant return type - reversed() returns SequencedSet,
     * not just SequencedCollection. This preserves type safety and allows
     * Set operations on the reversed view.
     */
    private static void demonstrateCovariantReturnType() {
        System.out.println("\n2. Covariant Return Type - Type Safety:");

        SequencedSet<String> languages = new LinkedHashSet<>();
        languages.add("Java");
        languages.add("Python");
        languages.add("JavaScript");

        System.out.println("Original set: " + languages);

        // reversed() returns SequencedSet, not just SequencedCollection
        SequencedSet<String> reversedSet = languages.reversed();
        System.out.println("Reversed view: " + reversedSet);

        // All Set methods available on reversed view (no casting needed)
        System.out.println("reversedSet.contains('Java'): " + reversedSet.contains("Java"));
        System.out.println("reversedSet.size(): " + reversedSet.size());

        // Modifications to reversed view affect original
        reversedSet.addFirst("Go");
        System.out.println("\nAfter reversedSet.addFirst('Go'):");
        System.out.println("  Original: " + languages);
        System.out.println("  Reversed: " + reversedSet);

        // Set semantics enforced on reversed view
        reversedSet.addFirst("Java");  // Duplicate - ignored
        System.out.println("\nAfter reversedSet.addFirst('Java'):");
        System.out.println("  Reversed: " + reversedSet + " (duplicate ignored)");
    }

    /**
     * Demonstrates the difference between insertion-ordered sets (LinkedHashSet)
     * and sorted sets (TreeSet). Both implement SequencedSet but have different
     * ordering strategies.
     *
     * Real-world scenario: User activity tracking
     * - LinkedHashSet: Track user actions in the order they occurred
     * - TreeSet: Display user actions sorted alphabetically for reports
     */
    private static void demonstrateInsertionVsSortedOrder() {
        System.out.println("\n3. Insertion Order vs Sorted Order:");
        System.out.println("Scenario: User Activity Tracking\n");

        // LinkedHashSet - maintains insertion order (chronological activity log)
        System.out.println("a) LinkedHashSet (Insertion Order - Activity Timeline):");
        SequencedSet<String> activityLog = new LinkedHashSet<>();
        activityLog.add("login");
        activityLog.add("view-product");
        activityLog.add("add-to-cart");
        activityLog.add("checkout");
        activityLog.add("view-product");  // Duplicate - ignored

        System.out.println("Activities in chronological order: " + activityLog);
        System.out.println("First activity: " + activityLog.getFirst() + " (session start)");
        System.out.println("Last activity: " + activityLog.getLast() + " (most recent)");

        // TreeSet - maintains sorted order (alphabetical for reports)
        System.out.println("\nb) TreeSet (Sorted Order - Alphabetical Report):");
        SequencedSet<String> sortedActivities = new TreeSet<>();
        sortedActivities.add("login");
        sortedActivities.add("view-product");
        sortedActivities.add("add-to-cart");
        sortedActivities.add("checkout");

        System.out.println("Activities sorted alphabetically: " + sortedActivities);
        System.out.println("First activity: " + sortedActivities.getFirst() + " (alphabetically first)");
        System.out.println("Last activity: " + sortedActivities.getLast() + " (alphabetically last)");

        // Both support reversed() with same API
        System.out.println("\nc) Reversed Views:");
        System.out.println("Timeline reversed (most recent first): " + activityLog.reversed());
        System.out.println("Alphabetical reversed (Z to A): " + sortedActivities.reversed());

        // Demonstrate getFirst/getLast work consistently
        System.out.println("\nd) Consistent API Across Both:");
        System.out.println("Timeline - first activity: " + activityLog.getFirst());
        System.out.println("Alphabetical - first activity: " + sortedActivities.getFirst());
        System.out.println("→ Same method, different ordering strategy!");
    }
}