package com.metaverse.msme.district_and_mandal.service;

import lombok.Data;

@Data
public class VillageResponse {
    private Integer villageId;
    private String villageName;
    private String aliasName;

    public VillageResponse(Integer villageId, String villageName) {
        this.villageId = villageId;
        this.villageName = villageName;
    }
}