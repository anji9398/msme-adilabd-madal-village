package com.metaverse.msme.address;

import java.util.Set;

public class AdminNameParts {

    private final String baseName;
    private final Set<String> qualifiers;

    public AdminNameParts(String baseName, Set<String> qualifiers) {
        this.baseName = baseName;
        this.qualifiers = qualifiers;
    }

    public String getBaseName() {
        return baseName;
    }

    public Set<String> getQualifiers() {
        return qualifiers;
    }
}
