package com.metaverse.msme.address;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AddressNormalizer {

    // ✅ Label / noise words (KEEP THIS SMALL & SAFE)
    private static final Set<String> STOP_WORDS = Set.of(
            // generic address labels
            "flat", "floor", "no", "dno", "door", "house",
            "building", "bldg", "apartment", "apt",

            // road / location labels
            "road", "rd", "street", "st", "lane", "ln",
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
    public Set<String> meaningfulTokenSet(String raw) {
        return new HashSet<>(extractMeaningfulTokens(raw));
    }


}
