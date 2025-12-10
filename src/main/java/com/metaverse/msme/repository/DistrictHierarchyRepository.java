package com.metaverse.msme.repository;

import com.metaverse.msme.model.DistrictHierarchyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DistrictHierarchyRepository
        extends JpaRepository<DistrictHierarchyEntity, Long> {

    Optional<DistrictHierarchyEntity> findByDistrictNameIgnoreCase(String districtName);

    @Query(value = "SELECT dh FROM DistrictHierarchyEntity dh WHERE dh.districtName = :districtName")
    Optional<DistrictHierarchyEntity> findByDistrictName(@Param("districtName") String districtName);
}

