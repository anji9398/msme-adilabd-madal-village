package com.metaverse.msme.controller;

import com.metaverse.msme.extractor.MandalDetectionResult;
import com.metaverse.msme.extractor.MandalDetector;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class MandalTestController {

    private final MandalDetector mandalDetector;

    public MandalTestController(MandalDetector mandalDetector) {
        this.mandalDetector = mandalDetector;
    }

    @GetMapping("/mandal")
    public MandalDetectionResult testMandal(
            @RequestParam String district,
            @RequestParam String address) {

        return mandalDetector.detectMandal(district, address);
    }
}
