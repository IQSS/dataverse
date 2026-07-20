package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helpers for simple admin settings that accept comma-separated lists (origins, methods, headers, etc.).
 * <p>
 * Behavior:
 * - Leading/trailing whitespace of the whole input is ignored.
 * - Whitespace immediately around commas is ignored ("GET, POST" == "GET,POST").
 * - Tokens are otherwise preserved exactly as typed (no quote stripping, no escape processing).
 * Not a full CSV parser: embedded commas, quoted fields with separators, and newlines inside tokens are NOT supported.
 */
public final class ListSplitUtil {
    /** Split on commas, trimming any adjacent to comma whitespace. */
    private static final Pattern SPLIT = Pattern.compile("\\s*,\\s*");

    /**
     * Split a comma-separated string into tokens preserving user input (beyond removing cosmetic
     * whitespace around commas and overall leading/trailing whitespace). Returns an empty list for
     * null or blank input.
     */
    public static List<String> split(final String rawCsv) {
        if (rawCsv == null) {
            return Collections.emptyList();
        }
        final String trimmedCsv = rawCsv.trim();
        if (trimmedCsv.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(SPLIT.split(trimmedCsv));
    }

    /** Convenience: split into a lowercase set. */
    public static Set<String> splitToLowerCaseSet(final String rawCsv) {
        if (rawCsv == null || rawCsv.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(split(rawCsv.toLowerCase()));
    }
}
