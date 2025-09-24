package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Configure CORS for API endpoints to allow frontend integration
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "file://")  // Fixed: Use allowedOriginPatterns instead of allowedOrigins
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // Configure static resource handling for demo UI files
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // Serve HTML files directly from root context
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/");
    }

    // Add view controllers for direct HTML access
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/cart").setViewName("forward:/shopping_cart.html");
        registry.addViewController("/payment").setViewName("forward:/payment_processing.html");
        registry.addViewController("/templates").setViewName("forward:/string_templates.html");
    }
}