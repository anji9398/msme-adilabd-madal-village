package com.metaverse.msme.extractor;

import java.util.Set;

public class VillageDetectionResult {

    private final VillageDetectionStatus status;
    private final String village;
    private final Set<String> matchedVillages;

    private VillageDetectionResult(VillageDetectionStatus status,
                                   String village,
                                   Set<String> matchedVillages) {
        this.status = status;
        this.village = village;
        this.matchedVillages = matchedVillages;
    }

    public static VillageDetectionResult single(String village) {
        return new VillageDetectionResult(
                VillageDetectionStatus.SINGLE_VILLAGE,
                village,
                null
        );
    }

    public static VillageDetectionResult multiple(Set<String> villages) {
        return new VillageDetectionResult(
                VillageDetectionStatus.MULTIPLE_VILLAGES,
                null,
                villages
        );
    }

    public static VillageDetectionResult notFound() {
        return new VillageDetectionResult(
                VillageDetectionStatus.VILLAGE_NOT_FOUND,
                null,
                null
        );
    }

    public VillageDetectionStatus getStatus() {
        return status;
    }

    public String getVillage() {
        return village;
    }

    public Set<String> getMatchedVillages() {
        return matchedVillages;
    }
}

