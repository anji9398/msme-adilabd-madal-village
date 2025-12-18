package com.metaverse.msme.service;


import com.metaverse.msme.extractor.*;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
public class AddressParseResult {

    private final MandalDetectionStatus mandalStatus;
    private final String mandal;
    private final Set<String> multipleMandals;

    private final VillageDetectionStatus villageStatus;
    private final String village;
    private final Set<String> multipleVillages;
/*

    private final DistrictDetectionStatus DistrictStatus;
    private final String district;
    private final Set<String> multipleDistricts;
*/

    private AddressParseResult(MandalDetectionStatus mandalStatus, String mandal, Set<String> multipleMandals, VillageDetectionStatus villageStatus, String village, Set<String> multipleVillages) {
        this.mandalStatus = mandalStatus;
        this.mandal = mandal;
        this.multipleMandals = multipleMandals;
        this.villageStatus = villageStatus;
        this.village = village;
        this.multipleVillages = multipleVillages;
    }

    /* --------------------------------------------
       ✅ Existing method (KEEP AS IS)
       -------------------------------------------- */
    public static AddressParseResult fromMandalResult(MandalDetectionResult m) {

        return new AddressParseResult(
                MandalDetectionStatus.MULTIPLE_MANDALS,
                m.getMandal(),
                m.getMatchedMandals(),
                null,
                null,
                null
        );
    }

    public static AddressParseResult fromDistrict(MandalDetectionResult m) {

        return new AddressParseResult(
                MandalDetectionStatus.MULTIPLE_DISTRICTS,
                m.getMandal(),
                m.getMatchedMandals(),
                null,
                null,
                null
        );
    }

    /* --------------------------------------------
       ✅ Existing method (KEEP AS IS)
       -------------------------------------------- */
    public static AddressParseResult combine(
            MandalDetectionResult m,
            VillageDetectionResult v) {

        return new AddressParseResult(
                m.getStatus(),
                m.getMandal(),
                null,
                v.getStatus(),
                v.getVillage(),
                v.getMatchedVillages()
        );
    }

    /* --------------------------------------------
       ✅ NEW METHOD — THIS IS WHAT YOU NEED
       -------------------------------------------- */
    public static AddressParseResult combineResolved(
            MandalDetectionResult mandalResult,
            String resolvedMandal,
            VillageDetectionResult villageResult) {

        return new AddressParseResult(
                mandalResult.getStatus(),
                resolvedMandal,                 // ✅ use resolved mandal
                mandalResult.getMatchedMandals(),
                villageResult.getStatus(),
                villageResult.getVillage(),
                villageResult.getMatchedVillages()
        );
    }


}
