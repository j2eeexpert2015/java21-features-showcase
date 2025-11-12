package org.example.concepts.sequencedcollection.basic;

import java.util.*;

public class BasicSequencedCollectionDemo {

    public static void main(String[] args) {
        System.out.println("=== BasicSequencedCollectionDemo ===");

        demonstrateSequencedCollectionInterface();
        demonstrateReversedView();
        System.out.println();
    }

    private static void demonstrateSequencedCollectionInterface() {
        System.out.println("1. SequencedCollection Interface Methods:");

        // Using LinkedList as it implements SequencedCollection
        SequencedCollection<String> items = new LinkedList<>();

        System.out.println("Starting with empty collection: " + items);

        // Add elements to both ends
        items.addLast("Middle");
        System.out.println("After addLast('Middle'): " + items);

        items.addFirst("First");
        System.out.println("After addFirst('First'): " + items);

        items.addLast("Last");
        System.out.println("After addLast('Last'): " + items);

        // Access elements from both ends
        System.out.println("getFirst(): " + items.getFirst());
        System.out.println("getLast(): " + items.getLast());

        // Remove elements from both ends
        String removedFirst = items.removeFirst();
        System.out.println("removeFirst() returned: " + removedFirst);
        System.out.println("Collection after removeFirst(): " + items);

        String removedLast = items.removeLast();
        System.out.println("removeLast() returned: " + removedLast);
        System.out.println("Collection after removeLast(): " + items);
    }

    private static void demonstrateReversedView() {
        System.out.println("\n2. Reversed View:");

        SequencedCollection<Integer> numbers = new LinkedList<>();
        numbers.addLast(1);
        numbers.addLast(2);
        numbers.addLast(3);
        numbers.addLast(4);

        System.out.println("Original collection: " + numbers);

        SequencedCollection<Integer> reversed = numbers.reversed();
        System.out.println("Reversed view: " + reversed);

        // Modifications to reversed view affect original
        reversed.addFirst(5);  // This adds to the end of original
        System.out.println("After reversed.addFirst(5):");
        System.out.println("  Original: " + numbers);
        System.out.println("  Reversed: " + reversed);
    }
}
