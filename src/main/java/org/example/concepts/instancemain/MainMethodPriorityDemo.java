package org.example.concepts.instancemain;

/**
 * Main Method Selection Priority Demo
 * 
 * When multiple main method signatures exist, Java uses this priority:
 * 
 *   1. static void main(String[] args)  ← Highest
 *   2. static void main()
 *   3. void main(String[] args)         ← Instance  
 *   4. void main()                      ← Lowest
 * 
 * Static and instance methods with same name/params are DIFFERENT methods
 * and CAN coexist. This class compiles successfully with all four.
 * 
 * Compile: javac --release 21 --enable-preview MainMethodPriorityDemo.java
 * Run: java --enable-preview org.example.concepts.instancemain.MainMethodPriorityDemo
 */
public class MainMethodPriorityDemo {
    /*

    // Priority 1: HIGHEST - This will be selected
    public static void main(String[] args) {
        System.out.println("Selected: static void main(String[] args)");
    }

    // Priority 2
    static void main() {
        System.out.println("Selected: static void main()");
    }

    // Priority 3
    void main(String[] args) {
        System.out.println("Selected: void main(String[] args)");
    }

    // Priority 4: LOWEST
    void main() {
        System.out.println("Selected: void main()");
    }
    */
}
