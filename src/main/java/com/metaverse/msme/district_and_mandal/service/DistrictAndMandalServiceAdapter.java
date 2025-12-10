package com.metaverse.msme.district_and_mandal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.msme.district_and_mandal.repository.DistrictRepository;
import com.metaverse.msme.model.DistrictHierarchyEntity;
import com.metaverse.msme.model.MsmeUnitDetails;
import com.metaverse.msme.repository.DistrictHierarchyRepository;
import com.metaverse.msme.repository.MsmeUnitDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistrictAndMandalServiceAdapter implements DistrictAndMandalService {

    private final DistrictRepository districtRepository;
    private final MsmeUnitDetailsRepository unitDetailsRepository;
    private HierarchyResponse hierarchy;
    private final DistrictHierarchyRepository districtHierarchyRepository;
    @Autowired
    private  ObjectMapper objectMapper;

    @Override
    public List<DistrictResponse> getAllDistricts() {
        return districtRepository.findAll()
                .stream()
                .map(d -> new DistrictResponse(d.getId(), d.getName()))
                .toList();
    }

    @Override
    public List<MandalResponse> getMandalsByDistrictName(String districtName) {
        // Get the district hierarchy
        DistrictHierarchyEntity district = districtHierarchyRepository.findByDistrictName(districtName)
                .orElseThrow(() -> new RuntimeException("District not found"));

        try {
            // Parse the hierarchy_json
            JsonNode root = objectMapper.readTree(district.getHierarchyJson());

            // Get mandals array
            JsonNode mandals = root.path("mandals");
            List<MandalResponse> result = new ArrayList<>();

            for (JsonNode mandal : mandals) {
                result.add(new MandalResponse(
                        mandal.path("mandalId").asLong(),
                        mandal.path("mandalName").asText()
                ));
            }

            return result;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse hierarchy JSON", e);
        }
    }



    @Override
    public List<VillageResponse> getVillagesByMandalName(String mandalName) {
        // Get the district hierarchy (e.g., Adilabad)
        DistrictHierarchyEntity district = districtHierarchyRepository.findByDistrictName("Adilabad")
                .orElseThrow(() -> new RuntimeException("District not found"));

        try {
            // Parse the hierarchy_json
            JsonNode root = objectMapper.readTree(district.getHierarchyJson());

            // Find the mandal node
            JsonNode mandals = root.path("mandals");
            for (JsonNode mandal : mandals) {
                if (mandal.path("mandalId").asText().equalsIgnoreCase(mandalName)) {
                    JsonNode villages = mandal.path("villages");
                    List<VillageResponse> result = new ArrayList<>();
                    for (JsonNode village : villages) {
                        result.add(new VillageResponse(
                                village.path("villageId").asInt(),
                                village.path("villageName").asText()
                        ));
                    }
                    return result;
                }
            }
            return Collections.emptyList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse hierarchy JSON", e);
        }
    }

    @Override
    public List<MsmeUnitDetails> getByVillage(String village, String mandal) {
        return unitDetailsRepository.findByVillageIgnoreCaseAndMandalIgnoreCase(village,mandal);
    }


}

