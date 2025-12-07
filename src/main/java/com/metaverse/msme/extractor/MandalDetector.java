package com.metaverse.msme.extractor;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.model.DistrictHierarchyEntity;
import com.metaverse.msme.repository.DistrictHierarchyRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MandalDetector {

    private final AddressNormalizer normalizer;
    private final DistrictHierarchyRepository repository;

    public MandalDetector(AddressNormalizer normalizer,
                          DistrictHierarchyRepository repository) {
        this.normalizer = normalizer;
        this.repository = repository;
    }

    public MandalDetectionResult detectMandal(String district, String rawAddress) {

        if (district == null || rawAddress == null) {
            return MandalDetectionResult.notFound();
        }

        // ✅ Step 1: Clean tokens
        Set<String> tokens = normalizer.meaningfulTokenSet(rawAddress);
        if (tokens.isEmpty()) {
            return MandalDetectionResult.notFound();
        }

        // ✅ Step 2: Load district hierarchy
        Optional<DistrictHierarchyEntity> opt =
                repository.findByDistrictNameIgnoreCase(district);

        if (opt.isEmpty()) {
            return MandalDetectionResult.notFound();
        }

        JSONObject root = new JSONObject(opt.get().getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        Set<String> exactMatches = new LinkedHashSet<>();
        Set<String> fuzzyMatches = new LinkedHashSet<>();

        // ✅ Step 3: Detect mandals (FIXED)
        for (int i = 0; i < mandalsArr.length(); i++) {

            JSONObject mandalObj = mandalsArr.getJSONObject(i);
            String mandalName = mandalObj.getString("mandalName");

            String mandalNorm = normalizer.normalize(mandalName); // e.g. "mavala new"
            Set<String> mandalWords =
                    new HashSet<>(Arrays.asList(mandalNorm.split(" ")));

            // ✅ EXACT MULTI-WORD MATCH
            if (tokens.containsAll(mandalWords)) {
                exactMatches.add(mandalName);
                continue;
            }

            // ✅ SAFE FUZZY MATCH (word-to-word)
            for (String mw : mandalWords) {
                for (String t : tokens) {
                    if (similarity(t, mw) >= 0.90) {
                        fuzzyMatches.add(mandalName);
                        break;
                    }
                }
            }
        }

        // ✅ Step 4: Decide result

        if (exactMatches.size() == 1) {
            return MandalDetectionResult.single(
                    exactMatches.iterator().next()
            );
        }

        if (exactMatches.size() > 1) {
            return MandalDetectionResult.multiple(exactMatches);
        }

        if (fuzzyMatches.size() == 1) {
            return MandalDetectionResult.single(
                    fuzzyMatches.iterator().next()
            );
        }

        if (fuzzyMatches.size() > 1) {
            return MandalDetectionResult.multiple(fuzzyMatches);
        }

        return MandalDetectionResult.notFound();
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
}
