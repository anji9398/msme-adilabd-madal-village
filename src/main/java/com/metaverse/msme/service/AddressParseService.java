package com.metaverse.msme.service;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.address.AdminNameParts;
import com.metaverse.msme.extractor.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AddressParseService {

    private final MandalDetector mandalDetector;
    private final VillageDetector villageDetector;
    private final AddressNormalizer addressNormalizer; // ✅ MUST EXIST

    public AddressParseService(
            MandalDetector mandalDetector,
            VillageDetector villageDetector,
            AddressNormalizer addressNormalizer) {   // ✅ MUST BE HERE

        this.mandalDetector = mandalDetector;
        this.villageDetector = villageDetector;
        this.addressNormalizer = addressNormalizer; // ✅ MUST ASSIGN
    }

    public AddressParseResult parse(String district, String address) {

        // 1️⃣ Detect mandal normally
        MandalDetectionResult mandalResult =
                mandalDetector.detectMandal(district, address);

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

        String norm = addressNormalizer.normalize(name);   // "mavala new"

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

        return new AdminNameParts(
                String.join(" ", baseParts).trim(),
                qualifiers
        );
    }
}