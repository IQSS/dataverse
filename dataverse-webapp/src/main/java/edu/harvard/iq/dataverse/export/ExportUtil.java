package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.openaire.FirstNames;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExportUtil {

    private static final Set<String> BLACKLISTED_WORDS = new HashSet<>(Arrays.asList("company", "university", "medical",
            "corporation", "institute", "foundation", "college", "school", "academy", "polytechnic", "hospital", "clinic",
            "research", "society", "lab", "laboratory", "library", "museum", "organization", "observatory", "service", "center"));
    private static final Set<String> BLACKLISTED_SIGNS = new HashSet<>(Arrays.asList("(", ")"));

    // -------------------- LOGIC --------------------

    public static boolean isOrganization(String text) {
        return !isPerson(text);
    }

    public static boolean isPerson(String text) {
        int commaCount = StringUtils.countMatches(text, ",");
        if (commaCount == 0) {
            text = text.trim();
            if (text.isEmpty()) {
                return false;
            }

            String[] parts = text.split("\\s+");
            int nameLength = parts.length;

            if (nameLength < 2 || nameLength > 4) {
                return false;
            }

            return isName(parts[0]) &&
                    Arrays.stream(parts).noneMatch(ExportUtil::isWordBlacklisted) &&
                    Arrays.stream(parts).noneMatch(ExportUtil::containsBlacklistedSigns);
        } else if (commaCount == 1 && !text.endsWith(",")) {
            String namesPart = text.split(",")[1];
            String[] words = namesPart.trim().split("\\s+");

            for (String word : words) {
                if (!isNameOrInitial(word)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static String normalizeAccents(String text) {
        return StringUtils.stripAccents(text)
                .replaceAll("Ł", "L")
                .replaceAll("ł", "l");
    }

    // -------------------- PRIVATE --------------------

    private static boolean isName(String word) {
        return FirstNames.getInstance().isFirstName(normalizeAccents(word));
    }

    private static boolean isWordBlacklisted(String word) {
        return BLACKLISTED_WORDS.contains(word.toLowerCase());
    }

    private static boolean containsBlacklistedSigns(String word) {
        return BLACKLISTED_SIGNS.contains(word);
    }

    private static boolean isNameOrInitial(String word) {

        if (StringUtils.isEmpty(word)) {
            return false;
        }
        if (isName(word)) {
            return true;
        }
        if (word.length() % 2 != 0) {
            return false;
        }

        for (int i = 0; i < word.length(); i += 2) {
            if (!isInitial(word.substring(i, i+2))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInitial(String word) {
        if (word.length() != 2) {
            return false;
        }

        return Character.isLetter(word.charAt(0)) && Character.isUpperCase(word.charAt(0)) && word.contains(".");
    }

}
