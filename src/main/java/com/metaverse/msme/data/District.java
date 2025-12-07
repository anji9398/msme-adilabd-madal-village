package com.metaverse.msme.data;

import java.util.Map;

public class District {
    private final String name;
    private final Map<String, Mandal> mandals;

    public District(String name, Map<String, Mandal> mandals) {
        this.name = name;
        this.mandals = mandals;
    }

    public String getName() {
        return name;
    }

    public Map<String, Mandal> getMandals() {
        return mandals;
    }
}

