package com.metaverse.msme.extractor;

import com.metaverse.msme.address.AddressNormalizer;
import com.metaverse.msme.model.DistrictHierarchyEntity;
import com.metaverse.msme.repository.DistrictHierarchyRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

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

        // -------------------- SAFETY CHECKS --------------------
        if (district == null || mandal == null || rawAddress == null) {
            return VillageDetectionResult.notFound();
        }

        List<String> tokens = normalizer.meaningfulTokenSet(rawAddress);
        if (tokens.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        Optional<DistrictHierarchyEntity> opt =
                repository.findByDistrictNameIgnoreCase(district);

        if (opt.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        JSONObject root = new JSONObject(opt.get().getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        // -------------------- FIND MANDAL --------------------
        JSONObject mandalObj = null;
        String mandalNorm = normalizer.normalize(mandal);   // exact normalize
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

        // -------------------- MATCH COLLECTION --------------------
        Set<String> exactMatches = new LinkedHashSet<>();
        Set<String> fuzzyMatches = new LinkedHashSet<>();
        String hqVillage = null;

        // STOP WORDS
        Set<String> STOP_WORDS = Set.of(
                phoneticNormalize(normalizer.normalize(district)),
                "city", "district", "dist", "block"
        );

        // -------------------- VILLAGE LOOP --------------------
        for (int i = 0; i < villagesArr.length(); i++) {

            JSONObject villageObj = villagesArr.getJSONObject(i);
            String villageName = villageObj.getString("villageName");

            String villageNorm = normalizer.normalize(villageName);
            String villagePhonetic = phoneticNormalize(villageNorm);

            // alias support
            String aliasNorm = null;
            String aliasPhonetic = null;

            if (villageObj.has("aliasName") && !villageObj.isNull("aliasName")) {
                aliasNorm = normalizer.normalize(villageObj.getString("aliasName"));
                aliasPhonetic = phoneticNormalize(aliasNorm);
            }

            // ------------------ FIXED HQ CHECK ------------------
            // HQ ONLY if villageName == mandalName OR alias == mandalName (exact normalized)
            boolean isHqVillage = villageNorm.equals(mandalNorm) || (aliasNorm != null && aliasNorm.equals(mandalNorm));

            if (isHqVillage) {
                hqVillage = villageName;
                continue;  // HQ fallback only
            }


            // ------------------ TOKEN MATCHING ------------------
            for (String token : tokens) {

                String tokenNorm = normalizer.normalize(token);
                String tokenPhonetic = phoneticNormalize(tokenNorm);

                if (STOP_WORDS.contains(tokenPhonetic)) continue;
                if (tokenPhonetic.length() <= 4) continue;

                // EXACT match (name or alias)
                if (tokenPhonetic.equals(villagePhonetic) || (aliasPhonetic != null && tokenPhonetic.equals(aliasPhonetic))) {
                    exactMatches.add(villageName);
                    break;
                }

                if (matchUpToThreeWords(tokens, villagePhonetic, aliasPhonetic)) {
                    exactMatches.add(villageName);
                }

                // FUZZY match (name or alias)
                if (similarity(tokenPhonetic, villagePhonetic) >= 0.90 || (aliasPhonetic != null && similarity(tokenPhonetic, aliasPhonetic) >= 0.90)) {
                    fuzzyMatches.add(villageName);
                    break;
                }
            }
        }

        // -------------------- DECISION BLOCK --------------------
        if (exactMatches.size() == 1) return VillageDetectionResult.single(exactMatches.iterator().next());

        if (exactMatches.size() > 1)  return VillageDetectionResult.multiple(exactMatches);

        if (fuzzyMatches.size() == 1)  return VillageDetectionResult.single(fuzzyMatches.iterator().next());

        if (fuzzyMatches.size() > 1) return VillageDetectionResult.multiple(fuzzyMatches);

        // HQ fallback ONLY if ZERO matches
        if (hqVillage != null) return VillageDetectionResult.single(hqVillage);

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
        return s.replace("oo", "u")
                .replace("oor", "ur");
    }

    private boolean matchUpToThreeWords(List<String> tokensPhonetic, String villagePhonetic, String aliasPhonetic) {
        int size = tokensPhonetic.size();

        String village = villagePhonetic.toLowerCase();
        String alias = aliasPhonetic != null ? aliasPhonetic.toLowerCase() : null;

        for (int i = 0; i < size; i++) {

            // -------- 1 WORD --------
            String one = tokensPhonetic.get(i).toLowerCase();
            if (one.equals(village) || (alias != null && one.equals(alias))) {
                return true;
            }

            // -------- 2 WORDS --------
            if (i + 1 < size) {
                String two = one + " " + tokensPhonetic.get(i + 1).toLowerCase();
                if (two.equals(village) || (alias != null && two.equals(alias))) {
                    return true;
                }
            }

            // -------- 3 WORDS --------
            if (i + 2 < size) {
                String three = one + " " + tokensPhonetic.get(i + 1).toLowerCase() + " " + tokensPhonetic.get(i + 2).toLowerCase();
                if (three.equals(village) || (alias != null && three.equals(alias))) {
                    return true;
                }
            }
        }
        return false;
    }

    public VillageDetectionResult detectVillageAcrossDistrict(
            String district,
            String rawAddress) {

        System.out.println("### detectVillageAcrossDistrict CALLED ###");

        String hqVillage = null;   // ✅ HQ fallback holder

        if (district == null || rawAddress == null) {
            return VillageDetectionResult.notFound();
        }

        List<String> tokens = normalizer.meaningfulTokenSet(rawAddress);
        if (tokens.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        Optional<DistrictHierarchyEntity> opt =
                repository.findByDistrictNameIgnoreCase(district);

        if (opt.isEmpty()) {
            return VillageDetectionResult.notFound();
        }

        JSONObject root = new JSONObject(opt.get().getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        // ✅ CASE-NORMALIZED SETS
        Set<String> exactMatches = new LinkedHashSet<>();
        Set<String> fuzzyMatches = new LinkedHashSet<>();

        // STOP WORDS
        Set<String> STOP_WORDS = Set.of(
                phoneticNormalize(normalizer.normalize(district)),
                "city", "district", "dist", "block", "mandal"
        );

        // 🔁 LOOP ALL MANDALS → ALL VILLAGES
        for (int m = 0; m < mandalsArr.length(); m++) {

            JSONObject mandalObj = mandalsArr.getJSONObject(m);
            String mandalName = mandalObj.getString("mandalName");

            String mandalNorm = phoneticNormalize(normalizer.normalize(mandalName));

            JSONArray villagesArr = mandalObj.getJSONArray("villages");

            for (int i = 0; i < villagesArr.length(); i++) {

                JSONObject villageObj = villagesArr.getJSONObject(i);
                String villageName = villageObj.getString("villageName");

                String villageNorm = normalizer.normalize(villageName);
                String villagePh = phoneticNormalize(villageNorm);

                String aliasNorm = null;
                String aliasPh = null;

                if (villageObj.has("aliasName") && !villageObj.isNull("aliasName")) {
                    aliasNorm = normalizer.normalize(villageObj.getString("aliasName"));
                    aliasPh = phoneticNormalize(aliasNorm);
                }

                // 🚫 HQ CHECK (MANDAL NAME == VILLAGE NAME)
                boolean isHqVillage = villagePh.equals(mandalNorm) || (aliasPh != null && aliasPh.equals(mandalNorm));

                if (isHqVillage) {
                       // remember HQ
                    continue;                  // 🚫 do NOT add to matches
                }

                // 🔎 TOKEN MATCHING
                for (String token : tokens) {

                    String tokenNorm =
                            normalizer.normalize(token);
                    String tokenPh =
                            phoneticNormalize(tokenNorm);

                    if (STOP_WORDS.contains(tokenPh)) continue;
                    if (tokenPh.length() <= 4) continue;

                    // EXACT MATCH
                    if (tokenPh.equals(villagePh) || (aliasPh != null && tokenPh.equals(aliasPh)) || matchUpToThreeWords(tokens, villagePh, aliasPh)) {
                        exactMatches.add(villageName.toUpperCase());
                        break;
                    }

                    // FUZZY MATCH
                    if (similarity(tokenPh, villagePh) >= 0.90
                            || (aliasPh != null && similarity(tokenPh, aliasPh) >= 0.90)) {

                        fuzzyMatches.add(villageName.toUpperCase());
                        break;
                    }
                }
            }
        }

        // ---------------- FINAL DECISION ----------------

        // 1️⃣ REAL VILLAGES FOUND → HQ MUST NOT COMPETE
        if (exactMatches.size() == 1) {
            return VillageDetectionResult.single(
                    exactMatches.iterator().next()
            );
        }

        if (exactMatches.size() > 1) {
            return VillageDetectionResult.multiple(exactMatches);
        }

        // 2️⃣ NO REAL VILLAGE → HQ FALLBACK
        if (exactMatches.isEmpty() && hqVillage != null) {
            return VillageDetectionResult.single(hqVillage);
        }

        // 3️⃣ FUZZY ONLY IF NO EXACT & NO HQ
        if (fuzzyMatches.size() == 1) {
            return VillageDetectionResult.single(
                    fuzzyMatches.iterator().next()
            );
        }

        if (fuzzyMatches.size() > 1) {
            return VillageDetectionResult.multiple(fuzzyMatches);
        }

        // 4️⃣ NOTHING FOUND
        return VillageDetectionResult.notFound();
    }

    public Set<String> findMandalsByVillage(
            String district,
            String villageName) {

        Set<String> mandals = new LinkedHashSet<>();

        Optional<DistrictHierarchyEntity> opt =
                repository.findByDistrictNameIgnoreCase(district);

        if (opt.isEmpty() || villageName == null) {
            return mandals;
        }

        JSONObject root = new JSONObject(opt.get().getHierarchyJson());
        JSONArray mandalsArr = root.getJSONArray("mandals");

        String targetVillagePh =
                phoneticNormalize(normalizer.normalize(villageName));

        for (int i = 0; i < mandalsArr.length(); i++) {

            JSONObject mandalObj = mandalsArr.getJSONObject(i);
            String mandalName = mandalObj.getString("mandalName");

            JSONArray villagesArr = mandalObj.getJSONArray("villages");

            for (int j = 0; j < villagesArr.length(); j++) {

                JSONObject v = villagesArr.getJSONObject(j);

                String villageNorm =
                        phoneticNormalize(
                                normalizer.normalize(v.getString("villageName")));

                String aliasNorm = null;
                if (v.has("aliasName") && !v.isNull("aliasName")) {
                    aliasNorm =
                            phoneticNormalize(
                                    normalizer.normalize(v.getString("aliasName")));
                }

                if (targetVillagePh.equals(villageNorm)
                        || (aliasNorm != null && targetVillagePh.equals(aliasNorm))) {

                    mandals.add(mandalName);
                }
            }
        }
        return mandals;
    }
}

