package com.metaverse.msme.repository;

import com.metaverse.msme.model.MsmeUnitDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MsmeUnitDetailsRepository extends JpaRepository<MsmeUnitDetails, Integer> {
    List<MsmeUnitDetails> findByVillageIgnoreCaseAndMandalIgnoreCase(String village, String mandal);
}