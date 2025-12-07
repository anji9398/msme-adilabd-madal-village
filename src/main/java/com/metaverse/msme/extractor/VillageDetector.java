package com.metaverse.msme.extractor;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.model.DistrictHierarchyEntity;
import com.metaverse.msme.repository.DistrictHierarchyRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class VillageDetector {

    private final AddressNormalizer normalizer;
    private final DistrictHierarchyRepository repository;

    public VillageDetector(AddressNormalizer normalizer,
                           DistrictHierarchyRepository repository) {
        this.normalizer = normalizer;
        this.repository = repository;
    }

    public VillageDetectionResult detectVillage(
            String district,
            String mandal,
            String rawAddress) {

        if (district == null || mandal == null || rawAddress == null) {
            return VillageDetectionResult.notFound();
        }

        // ✅ Normalize meaningful tokens from address
        Set<String> tokens = normalizer.meaningfulTokenSet(rawAddress);
        if (tokens.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        // ✅ Load district hierarchy
        Optional<DistrictHierarchyEntity> opt =
                repository.findByDistrictNameIgnoreCase(district);

        if (opt.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        JSONObject root = new JSONObject(opt.get().getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        // ✅ Find mandal object
        JSONObject mandalObj = null;
        String mandalNorm = normalizer.normalize(mandal);
        String mandalPhonetic = phoneticNormalize(mandalNorm);

        for (int i = 0; i < mandalsArr.length(); i++) {
            JSONObject m = mandalsArr.getJSONObject(i);
            if (normalizer.normalize(m.getString("mandalName"))
                    .equals(mandalNorm)) {
                mandalObj = m;
                break;
            }
        }

        if (mandalObj == null) {
            return VillageDetectionResult.notFound();
        }

        JSONArray villagesArr = mandalObj.getJSONArray("villages");
        String normalizedAddress = normalizer.normalize(rawAddress);

        Set<String> exactMatches = new LinkedHashSet<>();
        Set<String> fuzzyMatches = new LinkedHashSet<>();

        // ✅ Village detection loop
        for (int i = 0; i < villagesArr.length(); i++) {

            JSONObject villageObj = villagesArr.getJSONObject(i);

            String villageName = villageObj.getString("villageName");
            boolean isHQ = villageObj.optBoolean("isMandalHQ", false);

            String villageNorm =
                    phoneticNormalize(normalizer.normalize(villageName));

            // ✅ RULE 1: Mandal dominance (except HQ village)
            if (villageNorm.equals(mandalPhonetic) && !isHQ) {
                continue;
            }

            // ✅ RULE 2: Mandal HQ — direct full-address match
            if (isHQ && normalizedAddress.contains(
                    normalizer.normalize(villageName))) {
                exactMatches.add(villageName);
                continue;
            }
            System.out.println("Village JSON = " + villageObj);
            System.out.println("isMandalHQ = " + villageObj.optBoolean("isMandalHQ", false));


            // ✅ RULE 3: Token-based exact + fuzzy match
            for (String token : tokens) {

                String tokenNorm =
                        phoneticNormalize(normalizer.normalize(token));

                if (tokenNorm.equals(villageNorm)) {
                    exactMatches.add(villageName);
                    break;
                }

                if (similarity(tokenNorm, villageNorm) >= 0.90) {
                    fuzzyMatches.add(villageName);
                    break;
                }
            }
        }

        // ✅ DECISION BLOCK
        if (exactMatches.size() == 1) {
            return VillageDetectionResult.single(
                    exactMatches.iterator().next()
            );
        }

        if (exactMatches.size() > 1) {
            return VillageDetectionResult.multiple(exactMatches);
        }

        if (fuzzyMatches.size() == 1) {
            return VillageDetectionResult.single(
                    fuzzyMatches.iterator().next()
            );
        }

        if (fuzzyMatches.size() > 1) {
            return VillageDetectionResult.multiple(fuzzyMatches);
        }

        return VillageDetectionResult.notFound();
    }




    private double similarity(String s1, String s2) {
        int dist = levenshtein(s1, s2);
        int max = Math.max(s1.length(), s2.length());
        return max == 0 ? 1.0 : 1.0 - ((double) dist / max);
    }

    private int levenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private String phoneticNormalize(String s) {
        return s
                .replace("oo", "u")
                .replace("oor", "ur");
    }


}

