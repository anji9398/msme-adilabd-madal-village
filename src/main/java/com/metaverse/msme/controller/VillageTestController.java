package com.metaverse.msme.controller;

import com.metaverse.msme.extractor.VillageDetectionResult;
import com.metaverse.msme.extractor.VillageDetector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class VillageTestController {

    private final VillageDetector villageDetector;

    public VillageTestController(VillageDetector villageDetector) {
        this.villageDetector = villageDetector;
    }

    @GetMapping("/village")
    public VillageDetectionResult testVillage(
            @RequestParam String district,
            @RequestParam String mandal,
            @RequestParam String address) {

        return villageDetector.detectVillage(district, mandal, address);
    }
}

