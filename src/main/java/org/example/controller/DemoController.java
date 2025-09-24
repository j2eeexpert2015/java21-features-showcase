// Demo initialization endpoint
package org.example.controller;

import org.example.model.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin
public class DemoController {

    // Initialize demo with welcome message
    @GetMapping("/init")
    public ApiResponse initializeDemo() {
        return new ApiResponse("DemoController.initialize",
                "Shopping Cart Demo ready with Java 21 Sequenced Collections")
                .withServiceCall("DemoService.setupSequencedCollections",
                        List.of("LinkedHashSet", "ArrayList"));
    }
}