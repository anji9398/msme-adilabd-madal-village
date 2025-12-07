package com.metaverse.msme.service;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.address.AdminNameParts;
import com.metaverse.msme.extractor.*;
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

        // ✅ 1. Detect mandal
        MandalDetectionResult mandalResult =
                mandalDetector.detectMandal(district, address);

        if (mandalResult.getStatus() != MandalDetectionStatus.SINGLE_MANDAL) {
            return AddressParseResult.fromMandalResult(mandalResult);
        }

        // ✅ 2. Resolve mandal name dynamically (THIS IS NEW)
        String resolvedMandal =
                resolveMandalDisplayName(
                        mandalResult.getMandal(),
                        address
                );

        // ✅ 3. Detect village (use resolved mandal)
        VillageDetectionResult villageResult =
                villageDetector.detectVillage(
                        district,
                        resolvedMandal,
                        address
                );

        // ✅ 4. Build final response
        return AddressParseResult.combineResolved(
                mandalResult,
                resolvedMandal,
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
