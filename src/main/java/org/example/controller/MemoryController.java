package org.example.controller;

import org.example.service.MemoryService;
import org.example.service.MemoryService.AllocationResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Generate heap pressure — drives the entire GC comparison test.
     *
     * POST /api/memory/load/{shortLivedMB}/{survivorsMB}
     *
     * Example: POST /api/memory/load/160/40
     * - 160MB short-lived objects → immediate young-gen garbage
     * -  40MB survivors          → promoted to old gen, rotated out gradually
     *
     * All observability (GC pauses, heap, CPU, P99 latency) is handled
     * automatically by Micrometer + Spring Boot Actuator. No extra endpoints needed.
     */
    @PostMapping("/load/{shortLivedMB}/{survivorsMB}")
    public Map<String, Object> createLoad(
            @PathVariable int shortLivedMB,
            @PathVariable int survivorsMB) {

        AllocationResult result = memoryService.createNaturalWorkload(shortLivedMB, survivorsMB);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("totalObjects", result.totalObjects());
        response.put("youngObjects", result.youngObjects());
        response.put("survivorObjects", result.survivorObjects());
        response.put("totalMB", String.format("%.2f", result.totalMB()));
        response.put("durationMs", String.format("%.2f", result.durationMs()));

        return response;
    }
}