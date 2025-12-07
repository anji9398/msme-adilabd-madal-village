package com.metaverse.msme.data;

import java.util.Set;

public class Mandal {
    private final String name;
    private final Set<Village> villages;

    public Mandal(String name, Set<Village> villages) {
        this.name = name;
        this.villages = villages;
    }

    public String getName() {
        return name;
    }

    public Set<Village> getVillages() {
        return villages;
    }
}
