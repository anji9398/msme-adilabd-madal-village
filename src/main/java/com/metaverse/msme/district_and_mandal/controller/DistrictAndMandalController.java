package com.metaverse.msme.district_and_mandal.controller;

import com.metaverse.msme.district_and_mandal.service.DistrictAndMandalService;
import com.metaverse.msme.district_and_mandal.service.DistrictResponse;
import com.metaverse.msme.district_and_mandal.service.MandalResponse;
import com.metaverse.msme.district_and_mandal.service.VillageResponse;
import com.metaverse.msme.model.MsmeUnitDetails;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class DistrictAndMandalController {

    private final DistrictAndMandalService districtAndMandalService;

    @GetMapping("/districts")
    public List<DistrictResponse> getDistricts() {
        return districtAndMandalService.getAllDistricts();
    }

    @GetMapping("/mandals/{districtId}")
    public List<MandalResponse> getMandals(@PathVariable String districtId) {
        return districtAndMandalService.getMandalsByDistrictName(districtId);
    }

    @GetMapping("/villages/{mandalId}")
    public List<VillageResponse> getVillages(@PathVariable String mandalId) {
        return districtAndMandalService.getVillagesByMandalName(mandalId);
    }

    @GetMapping("/units")
    public ResponseEntity<List<MsmeUnitDetails>> getByVillage(@PathParam("village") String village,
                                                              @PathParam("mandal") String madal) {
        List<MsmeUnitDetails> details = districtAndMandalService.getByVillage(village,madal);
        if (details.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(details);
    }
}

