package com.metaverse.msme.service;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.address.AdminNameParts;
import com.metaverse.msme.extractor.*;
import com.metaverse.msme.model.MsmeUnitDetails;
import com.metaverse.msme.repository.MsmeUnitDetailsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AddressParseService {

    private final MandalDetector mandalDetector;
    private final VillageDetector villageDetector;
    private final AddressNormalizer addressNormalizer; // ✅ MUST EXIST
    private final MsmeUnitDetailsRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private  EntityManager entityManager;
    public AddressParseService(
            MandalDetector mandalDetector,
            VillageDetector villageDetector,
            AddressNormalizer addressNormalizer, MsmeUnitDetailsRepository repository) {

        this.mandalDetector = mandalDetector;
        this.villageDetector = villageDetector;
        this.addressNormalizer = addressNormalizer; // ✅ MUST ASSIGN
        this.repository = repository;
    }

    public AddressParseResult parse(String district, String address) {

        // 1️⃣ Detect mandal normally
        MandalDetectionResult mandalResult = mandalDetector.detectMandal(district, address);

    /* ----------------------------------------------------------
       CASE 1 & CASE 2 HANDLING
       ---------------------------------------------------------- */
        if (mandalResult.getStatus() == MandalDetectionStatus.MANDAL_NOT_FOUND) {

            // ⭐ Fallback mandal = district HQ mandal (example: Adilabad)
            String fallbackMandal = district;

            // Try detecting village under fallback mandal
            VillageDetectionResult fallbackVillage =
                    villageDetector.detectVillage(
                            district,
                            fallbackMandal,
                            address
                    );

            // ⭐ CASE 2 → Valid village FOUND under fallback mandal
            if (fallbackVillage.getStatus() == VillageDetectionStatus.SINGLE_VILLAGE
                    || fallbackVillage.getStatus() == VillageDetectionStatus.MULTIPLE_VILLAGES) {

                // Create a fake MandalDetectionResult to satisfy combineResolved()
                MandalDetectionResult fakeMandalResult =
                        MandalDetectionResult.single(fallbackMandal);

                return AddressParseResult.combineResolved(
                        fakeMandalResult,
                        fallbackMandal,       // mandal = Adilabad
                        fallbackVillage       // village = Mallapur
                );
            }

            // ⭐ CASE 1 → No village found even under fallback mandal
            // Use existing factory method — gives MANDAL_NOT_FOUND + null village
            return AddressParseResult.fromMandalResult(mandalResult);
        }

    /* ----------------------------------------------------------
       NORMAL FLOW → Mandal already found
       ---------------------------------------------------------- */

        if (mandalResult.getStatus() != MandalDetectionStatus.SINGLE_MANDAL) {
            return AddressParseResult.fromMandalResult(mandalResult);
        }

        // 2️⃣ Use DB mandal name for village detection
        String dbMandal = mandalResult.getMandal();

        VillageDetectionResult villageResult =
                villageDetector.detectVillage(
                        district,
                        dbMandal,
                        address
                );

        // 3️⃣ Resolve mandal display-friendly version
        String displayMandal =
                resolveMandalDisplayName(dbMandal, address);

        return AddressParseResult.combineResolved(
                mandalResult,
                displayMandal,
                villageResult
        );
    }

    // ✅ YOUR DYNAMIC METHOD LIVES HERE
    private String resolveMandalDisplayName(
            String dbMandalName,
            String address) {

        AdminNameParts dbParts = parseAdminName(dbMandalName);
        Set<String> addrWords = new HashSet<>(
                Arrays.asList(
                        addressNormalizer.normalize(address).split(" ")
                )
        );

        for (String q : dbParts.getQualifiers()) {
            if (addrWords.contains(q)) {
                return dbMandalName;       // "Mavala (New)"
            }
        }

        return capitalize(dbParts.getBaseName()); // "Mavala"
    }

    private String capitalize(String s) {
        String[] arr = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : arr) {
            sb.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    public AdminNameParts parseAdminName(String name) {

        String norm = addressNormalizer.normalize(name);

        String[] words = norm.split(" ");

        Set<String> qualifiers = new LinkedHashSet<>();
        List<String> baseParts = new ArrayList<>();

        for (String w : words) {
            // qualifier = word originally inside brackets in DB
            if (name.toLowerCase().contains("(" + w + ")")) {
                qualifiers.add(w);
            } else {
                baseParts.add(w);
            }
        }
        return new AdminNameParts(String.join(" ", baseParts).trim(), qualifiers);
    }

    @Transactional
    public int updateAllUnitsVillage() {

        int page = 0;
        int size = 2000;                // BATCH SIZE
        int totalUpdated = 0;

        Page<MsmeUnitDetails> pageResult;

        // Cache to avoid parsing same address again
        Map<String, AddressParseResult> cache = new ConcurrentHashMap<>();

        do {
            pageResult = repository.findAll(PageRequest.of(page, size));

            Map<Integer, String> updateMap = new ConcurrentHashMap<>();

            // PARALLEL PROCESSING
            pageResult.getContent().parallelStream().forEach(unit -> {

                if (unit.getUnitAddress() == null) return;

                // CACHE: Parse once for repeated addresses
                AddressParseResult result = cache.computeIfAbsent(
                        unit.getUnitAddress(),
                        addr -> parse("Adilabad", addr)
                );

                if (result != null && result.getVillage() != null) {
                    updateMap.put(unit.getSlno(), result.getVillage());
                }
            });

            // BULK UPDATE
            if (!updateMap.isEmpty()) {
                batchUpdateVillage(updateMap);
                totalUpdated += updateMap.size();
            }

            System.out.println("Batch " + page + " completed. Updated: " + totalUpdated);

            page++;

        } while (!pageResult.isLast());

        return totalUpdated;
    }



    public void batchUpdateVillage(Map<Integer, String> updates) {

        String sql = "UPDATE msme_unit_details SET villageId = ? WHERE slno = ?";

        jdbcTemplate.batchUpdate(sql, updates.entrySet(), 500,
                (ps, entry) -> {
                    ps.setString(1, entry.getValue());
                    ps.setInt(2, entry.getKey());
                }
        );
    }
}