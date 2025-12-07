package com.metaverse.msme.repository;

import com.metaverse.msme.model.DistrictHierarchyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistrictHierarchyRepository
        extends JpaRepository<DistrictHierarchyEntity, Long> {

    Optional<DistrictHierarchyEntity> findByDistrictNameIgnoreCase(String districtName);

}

