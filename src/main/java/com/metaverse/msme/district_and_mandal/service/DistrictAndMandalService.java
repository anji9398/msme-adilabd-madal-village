package com.metaverse.msme.district_and_mandal.service;

import com.metaverse.msme.model.MsmeUnitDetails;

import java.util.List;

public interface DistrictAndMandalService {
    List<DistrictResponse> getAllDistricts();
    List<MandalResponse> getMandalsByDistrictName(String districtId);
    List<MsmeUnitDetails> getByVillage(String village, String mandal);
    List<VillageResponse> getVillagesByMandalName(String mandalName);
}
