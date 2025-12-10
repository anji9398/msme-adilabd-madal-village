package com.metaverse.msme.address;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AddressNormalizer {

    // ✅ Label / noise words (KEEP THIS SMALL & SAFE)
    private static final Set<String> STOP_WORDS = Set.of(
            // generic address labels
            "flat", "floor", "no", "dno", "door", "house",
            "building", "bldg", "apartment", "apt",

            // road / location labels
            "rd", "street", "st", "lane", "ln",
            "near", "beside", "behind", "opp", "opposite",

            // administrative labels
            "village", "vill", "v",
            "town", "city",
            "block", "blk",
            "dist", "district",
            "mandal", "mdl", "m"
    );

    /**
     * 1) Lowercase
     * 2) Remove non-letters/digits
     * 3) Collapse spaces
     */
    public String normalize(String raw) {
        if (raw == null) return "";

        return raw
                .toLowerCase()
                // keep text, remove brackets themselves but NOT the words
                .replaceAll("[()]", " ")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * ✅ Core method
     * Returns ONLY meaningful tokens (no labels)
     *
     * Example:
     * input:
     *   "flat no building muthnoor road street muthnoor village town muthoor block muthnoor city indervelly"
     *
     * output:
     *   ["muthnoor", "muthnoor", "muthoor", "muthnoor", "indervelly"]
     */
    public List<String> extractMeaningfulTokens(String raw) {
        String norm = normalize(raw);
        if (norm.isEmpty()) return List.of();

        String[] tokens = norm.split(" ");

        return Arrays.stream(tokens)
                .filter(t -> !STOP_WORDS.contains(t)) // ✅ remove labels
                .collect(Collectors.toList());
    }

    /**
     * ✅ Convenience – cleaned meaningful string
     */
    public String cleanedMeaningfulString(String raw) {
        return String.join(" ", extractMeaningfulTokens(raw));
    }

    /**
     * ✅ NEW (important for next steps)
     * Unique token set for O(1) lookups (mandal/village detection)
     */
    public List<String> meaningfulTokenSet(String raw) {

        if (raw == null || raw.isBlank()) return Collections.emptyList();

        // Normalize (lowercase, remove punctuation)
        String norm = normalize(cleanRawAddress(raw));

        // Split into tokens
        String[] parts = norm.split("\\s+");

        // Words that should NEVER participate in mandal/village detection
        Set<String> STOP = Set.of(
                "village", "town", "city", "district", "dist", "mandal",
                "block", "street", "st", "rd", "lane", "colony",
                "house", "flat", "building", "doorno", "plot", "near", "opp",
                "area", "locality", "0", "-"
        );

        List<String> out = new ArrayList<>();
//        out = normalizeAndRemoveRoadNames(out);
        for (String p : parts) {
            if (p == null) continue;
            p = p.trim();
            if (p.isEmpty()) continue;
            if (STOP.contains(p)) continue;
            if (p.length() <= 1) continue;
            if (p.matches("^[0-9]+$")) continue; // remove pure numbers

            out.add(p);
        }
        return out;
    }


    private String cleanRawAddress(String raw) {

        if (raw == null) return "";

        String lower = raw.toLowerCase();

        // ✅ CASE 1: Structured form (Road/Street:- exists)
        if (lower.matches(".*road\\s*/\\s*street\\s*:-?.*")) {
            // Remove values ONLY inside Road/Street field
            lower = lower.replaceAll("road\\s*/\\s*street\\s*:-?\\s*[^,]+", " ");

            // Remove other labels but KEEP their values
            lower = lower.replaceAll("\\b(flat\\s*no|building|village/town|block|city)\\s*:-?", " ");
        }// ✅ CASE 2: Free-text address (no labels)
        else {
            // Remove road-related words globally
            lower = lower.replaceAll("\\b(road|street|lane|rd|st|colony|area|block)\\b", " ");

            // Remove (V), (H) etc
            lower = lower.replaceAll("\\([a-z]+\\)", " ");
        }

        // ✅ COMMON CLEANUP
        lower = lower.replaceAll("\\b(h\\.?no|d\\.?no|plot|flat)\\b\\s*[:\\-]?\\s*\\w+", " ");

        lower = lower.replaceAll("[^a-z\\s]", " ");
        lower = lower.replaceAll("\\s+", " ").trim();

        return lower;
    }

/*

    List<String> normalizeAndRemoveRoadNames(List<String> tokens) {

        List<String> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {

            String token = tokens.get(i);
            String norm = phoneticNormalize(token).toLowerCase();

            System.out.println("Token: " + token + " | Normalized: " + norm);

            // If current token is ROAD → skip it
            if (ROAD_WORDS.contains(norm)) {

                System.out.println("➡ ROAD word detected: " + token);

                // Also remove the PREVIOUS token safely
                if (!result.isEmpty()) {

                    String prev = phoneticNormalize(result.get(result.size() - 1)).toLowerCase();
                    String prevRaw = result.get(result.size() - 1);

                    System.out.println("   Checking previous token: " + prevRaw + " | normalized: " + prev);

                    // remove only if it looks like a road name
                    if (prev.length() >= 4 && !VILLAGE_MARKERS.contains(prev)) {

                        System.out.println("   ❌ Removing previous token (belongs to ROAD): " + prevRaw);
                        result.remove(result.size() - 1);
                    } else {
                        System.out.println("   ✔ Previous token kept (marker or short): " + prevRaw);
                    }
                }

                System.out.println("   ❌ Skipping ROAD token: " + token);
                continue; // skip ROAD itself
            }

            // Token accepted
            System.out.println("✔ Keeping token: " + token);
            result.add(token);
        }

        System.out.println("Final Tokens After Road Removal: " + result);
        return result;
    }


    private String phoneticNormalize(String s) {
        return s
                .replace("oo", "u")
                .replace("oor", "ur");
    }

 */
}
