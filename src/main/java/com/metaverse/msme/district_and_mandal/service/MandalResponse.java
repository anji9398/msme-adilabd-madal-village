package com.metaverse.msme.district_and_mandal.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class MandalResponse {
    private Long mandalId;
    private String mandalName;
    private String aliasName;

    public MandalResponse(String aliasName) {
        this.aliasName = aliasName;
    }

    private List<VillageResponse> villages;

    public MandalResponse(Long id, String name) {
        this.mandalId = id;
        this.mandalName = name;
    }
}