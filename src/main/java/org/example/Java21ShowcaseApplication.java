package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the TechMart Java 21 Features Showcase application.
 *
 * This class initializes and runs the Spring Boot application, which serves
 * the REST APIs and the interactive frontend demo.
 */
@SpringBootApplication
public class Java21ShowcaseApplication {

    // The main method is the standard entry point for a Java application.
    // SpringApplication.run() bootstraps the entire Spring container, starts
    // the embedded web server (Tomcat), and deploys the application.
    public static void main(String[] args) {
        // Log statements to provide feedback during startup.
        System.out.println("🚀 Starting TechMart Java 21 Features Showcase...");
        SpringApplication.run(Java21ShowcaseApplication.class, args);
        System.out.println("✅ TechMart application is ready!");
    }
}