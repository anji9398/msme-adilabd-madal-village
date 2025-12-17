package com.metaverse.msme.extractor;

import java.util.Set;

public class MandalDetectionResult {

    private final MandalDetectionStatus status;
    private final String mandal;
    private final Set<String> matchedMandals;

    private MandalDetectionResult(
            MandalDetectionStatus status,
            String mandal,
            Set<String> matchedMandals) {
        this.status = status;
        this.mandal = mandal;
        this.matchedMandals = matchedMandals;
    }

    public static MandalDetectionResult single(String mandal) {
        return new MandalDetectionResult(
                MandalDetectionStatus.SINGLE_MANDAL,
                mandal,
                null
        );
    }

    public static MandalDetectionResult multiple(Set<String> mandals) {
        return new MandalDetectionResult(
                MandalDetectionStatus.MULTIPLE_MANDALS,
                null,
                mandals
        );
    }

    public static MandalDetectionResult multipleDistricts(Set<String> mandals) {
        return new MandalDetectionResult(
                MandalDetectionStatus.MULTIPLE_DISTRICTS,
                null,
                mandals
        );
    }

    public static MandalDetectionResult notFound() {
        return new MandalDetectionResult(
                MandalDetectionStatus.MANDAL_NOT_FOUND,
                null,
                null
        );
    }

    public MandalDetectionStatus getStatus() {
        return status;
    }

    public String getMandal() {
        return mandal;
    }

    public Set<String> getMatchedMandals() {
        return matchedMandals;
    }
}

