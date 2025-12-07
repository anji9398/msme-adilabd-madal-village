package com.metaverse.msme.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "district_hierarchy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistrictHierarchyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "district_name", unique = true, nullable = false)
    private String districtName;

    @Column(name = "hierarchy_json", columnDefinition = "jsonb", nullable = false)
    private String hierarchyJson;
}

