/**
 * Unnamed Class Example - From Blog
 * 
 * No class keyword. No package statement. The file can have any name.
 * 
 * The compiler generates:
 *   final class Greeting {
 *       private String name = "Alice";
 *       private int age = 25;
 *       Greeting() { }
 *       void sayHello() { ... }
 *       void main() { ... }
 *   }
 * 
 * Run: java --enable-preview --source 21 Greeting.java
 */

private String name = "Alice";
private int age = 25;

void sayHello() {
    System.out.println("Hello, I'm " + name + " and I'm " + age);
}

void main() {
    sayHello();
}
