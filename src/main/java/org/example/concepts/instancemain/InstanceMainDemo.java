package org.example.concepts.instancemain;

/**
 * Instance Main Method Demo - From Blog Example
 * 
 * With instance main methods, you can access instance fields and methods
 * directly — no need to create an object first. Java handles the 
 * instantiation for you.
 * 
 * Compile: javac --release 21 --enable-preview InstanceMainDemo.java
 * Run: java --enable-preview org.example.concepts.instancemain.InstanceMainDemo
 */
public class InstanceMainDemo {
    
    private String greeting = "Hello from instance!";
    
    void main() {
        System.out.println(greeting);  // Direct field access — no static restriction
    }
}
