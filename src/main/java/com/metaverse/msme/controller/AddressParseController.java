package com.metaverse.msme.controller;

import com.metaverse.msme.service.AddressParseResult;
import com.metaverse.msme.service.AddressParseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/address")
public class AddressParseController {

    private final AddressParseService service;

    public AddressParseController(AddressParseService service) {
        this.service = service;
    }

    @Operation(summary = "Detect mandal and village from address")
    @GetMapping("/parse")
    public AddressParseResult parseAddress(
            @RequestParam String district,
            @RequestParam String address) {

        return service.parse("Adilabad", address);
    }
}