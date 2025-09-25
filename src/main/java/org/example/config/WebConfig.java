package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * Web configuration for the Java 21 Features Showcase application.
 *
 * This configuration class handles:
 * - CORS settings for API endpoints to allow frontend integration
 * - Static resource serving for HTML, CSS, JS files
 * - View controller mappings for direct page access
 * - Root path redirection to the main demo landing page (localhost:8080 → index.html)
 *
 * Key Features:
 * - Enables cross-origin requests for API endpoints
 * - Serves static content from classpath:/static/
 * - Provides clean URL mappings for demo pages
 * - Ensures localhost:8080 automatically loads the main demo page
 *
 * @author TechMart Demo Team
 * @since Java 21
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure Cross-Origin Resource Sharing (CORS) for API endpoints.
     *
     * This configuration allows the frontend HTML pages to make AJAX calls
     * to the backend APIs running on localhost during development and testing.
     *
     * Supported origins: localhost on any port, file:// protocol for local files
     * Supported methods: GET, POST, PUT, DELETE, OPTIONS
     * Cache duration: 1 hour (3600 seconds)
     *
     * @param registry the CORS registry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "file://")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Configure static resource handling for demo UI files.
     *
     * This setup provides multiple resource handlers to ensure all static content
     * is properly served from the classpath:/static/ directory:
     *
     * 1. /static/** - Traditional static assets (CSS, JS, images)
     * 2. /*.html - Direct HTML file access
     * 3. /** - Catch-all for any other static resources (ENSURES localhost:8080 WORKS)
     *
     * The catch-all handler (/**) is crucial for making localhost:8080 work properly
     * by allowing Spring to serve index.html from the root path.
     *
     * @param registry the resource handler registry to configure
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Traditional static assets with caching
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // Direct HTML file access
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/");

        // CRITICAL: Catch-all handler for root path static resources
        // This ensures localhost:8080 can serve index.html properly
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

    /**
     * Add view controllers for direct HTML access and clean URL navigation.
     *
     * These mappings provide user-friendly URLs for accessing different demo pages:
     *
     * - localhost:8080/          → index.html (Main Java 21 Features landing page)
     * - localhost:8080/cart      → shopping_cart.html (Sequenced Collections demo)
     * - localhost:8080/payment   → payment_processing.html (Pattern Matching demo)
     * - localhost:8080/templates → string_templates.html (String Templates demo)
     *
     * Using 'forward:' maintains clean URLs in the browser address bar while
     * serving the correct HTML content from static resources.
     *
     * The root path mapping ("/") is the KEY FIX that ensures localhost:8080
     * automatically displays the main demo page.
     *
     * @param registry the view controller registry to configure
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 🎯Root path → Main demo landing page
        // This is what makes localhost:8080 automatically show index.html
        registry.addViewController("/").setViewName("forward:/index.html");

        // Clean URL shortcuts for individual demo pages
        registry.addViewController("/cart").setViewName("forward:/shopping_cart.html");
        registry.addViewController("/payment").setViewName("forward:/payment_processing.html");
        registry.addViewController("/templates").setViewName("forward:/string_templates.html");
    }
}