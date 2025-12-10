package com.metaverse.msme.extractor;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.model.DistrictHierarchyEntity;
import com.metaverse.msme.repository.DistrictHierarchyRepository;
import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MandalDetector {

    private final AddressNormalizer normalizer;
    private final DistrictHierarchyRepository repository;
    private final Map<String, DistrictHierarchyEntity> extractorCache = new HashMap<>();

    public MandalDetector(AddressNormalizer normalizer,
                          DistrictHierarchyRepository repository) {
        this.normalizer = normalizer;
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        List<DistrictHierarchyEntity> docs = repository.findAll();
        for (DistrictHierarchyEntity d : docs) {
            extractorCache.put(d.getDistrictName(),d);
        }
}

    public MandalDetectionResult detectMandal(String district, String rawAddress) {

        if (district == null || rawAddress == null) {
            return MandalDetectionResult.notFound();
        }

    /* ---------------------------------------------------------
       STEP 1 — CLEAN TOKENS (from normalizer)
       --------------------------------------------------------- */
        Set<String> rawTokens = normalizer.meaningfulTokenSet(rawAddress);


        System.out.println("RAW TOKENS = " + rawTokens);

        for (String tok : rawTokens) {
            String n = normalizer.normalize(tok);
            String p = phoneticNormalize(n);
            System.out.println("TOKEN => raw: [" + tok + "], norm: [" + n + "], phonetic: [" + p + "]");
        }


        // Convert tokens to phonetic-normalized clean tokens
        Set<String> tokens = new HashSet<>();

        String districtNormPh = phoneticNormalize(normalizer.normalize(district));

        // Extra stopwords specifically for mandal detection
        Set<String> EXTRA_STOP = Set.of(
                "city", "district", "town", "village",
                "block", "road", "street"
        );

        for (String tok : rawTokens) {
            String t = phoneticNormalize(normalizer.normalize(tok));

            if (t.isBlank()) continue;

            // Remove junk words
            if (EXTRA_STOP.contains(t)) continue;

            // ❌ IMPORTANT FIX:
            // Remove token equal to district HQ mandal name
            // Prevents CITY=ADILABAD from becoming mandal=Adilabad
            if (t.equals(districtNormPh)) continue;

            tokens.add(t);
        }

        if (tokens.isEmpty()) {
            return MandalDetectionResult.notFound();
        }

    /* ---------------------------------------------------------
       STEP 2 — READ DISTRICT HIERARCHY
       --------------------------------------------------------- */
        DistrictHierarchyEntity opt = extractorCache.get("Adilabad");


        JSONObject root = new JSONObject(opt.getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        Set<String> exactMatches = new LinkedHashSet<>();
       Set<String> fuzzyMatches = new LinkedHashSet<>();

        String hqExactCandidate = null;
       String hqFuzzyCandidate = null;

    /* ---------------------------------------------------------
       STEP 3 — DETECT MANDAL
       --------------------------------------------------------- */
        for (int i = 0; i < mandalsArr.length(); i++) {

            JSONObject mandalObj = mandalsArr.getJSONObject(i);

            String mandalName = mandalObj.getString("mandalName");

            // NAME normalized & phonetic
            String mandalNorm = phoneticNormalize(
                    normalizer.normalize(mandalName));

            // ALIAS normalized & phonetic
            String aliasNorm = null;
            if (mandalObj.has("alisaName") && !mandalObj.isNull("aliasName")) {
                aliasNorm = phoneticNormalize(
                        normalizer.normalize(mandalObj.getString("aliasName")));
            }

            // split words like "adilabad rural"
            Set<String> mandalWords =
                    new HashSet<>(Arrays.asList(mandalNorm.split("\\s+")));

            boolean isDistrictHQ = mandalNorm.equals(districtNormPh);

            /* ------------------ EXACT MATCH ---------------------- */

            boolean exactMatched =
                    tokens.containsAll(mandalWords) ||
                            (aliasNorm != null && tokens.contains(aliasNorm));

            if (exactMatched) {

                if (isDistrictHQ) {
                    // Adilabad mandal found → fallback only
                    hqExactCandidate = mandalName;
                } else {
                    exactMatches.add(mandalName);
                }
                continue;
            }

            /* ------------------ FUZZY MATCH ---------------------- */

            for (String t : tokens) {

                // fuzzy by name words
                for (String mw : mandalWords) {
                    if (similarity(t, mw) >= 0.90) {
                        if (isDistrictHQ) {
                            hqFuzzyCandidate = mandalName;
                        } else {
                            fuzzyMatches.add(mandalName);
                        }
                        break;
                    }
                }

                // fuzzy by alias
                if (aliasNorm != null &&
                        similarity(t, aliasNorm) >= 0.90) {

                    if (isDistrictHQ) {
                        hqFuzzyCandidate = mandalName;
                    } else {
                        fuzzyMatches.add(mandalName);
                    }
                    break;
                }
            }

        }

    /* ---------------------------------------------------------
       STEP 4 — FINAL DECISION (ORDER MATTERS)
       --------------------------------------------------------- */

        // 1️⃣ NON-HQ EXACT WINNER
        if (exactMatches.size() == 1) {
            return MandalDetectionResult.single(
                    exactMatches.iterator().next());
        }

        if (exactMatches.size() > 1) {
            return MandalDetectionResult.multiple(exactMatches);
        }

        // 2️⃣ NON-HQ FUZZY WINNER
        if (fuzzyMatches.size() == 1) {
            return MandalDetectionResult.single(
                    fuzzyMatches.iterator().next());
        }

        if (fuzzyMatches.size() > 1) {
            return MandalDetectionResult.multiple(fuzzyMatches);
        }

// ⚠️ If tokens do NOT contain mandal-like words → do NOT fallback to HQ
        if (!tokens.isEmpty()) {
            if (hqExactCandidate != null) {
                return MandalDetectionResult.single(hqExactCandidate);
            }
             if (hqFuzzyCandidate != null) {
                return MandalDetectionResult.single(hqFuzzyCandidate);
            }
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


    private boolean matchesByNameOrAlias(
            String tokenNorm,
            String nameNorm,
            String aliasRaw) {

        // ✅ direct name match
        if (tokenNorm.equals(nameNorm)) {
            return true;
        }

        // ✅ alias check
        if (aliasRaw == null || aliasRaw.isBlank()) {
            return false;
        }

        String[] aliases = aliasRaw.split(",");

        for (String a : aliases) {
            String aliasNorm =
                    phoneticNormalize(normalizer.normalize(a.trim()));

            if (tokenNorm.equals(aliasNorm)) {
                return true;
            }
        }

        return false;
    }

    private String phoneticNormalize(String s) {
        return s
                .replace("oo", "u")
                .replace("oor", "ur");
    }

}

