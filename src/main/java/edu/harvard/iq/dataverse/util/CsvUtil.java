package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Minimal helpers for admin-entered comma separated lists of simple tokens.
 * Not a general CSV parser: no support for embedded commas, escapes, or newlines.
 */
public final class CsvUtil {
    /** Split on commas, trimming any adjacent to comma whitespace. */
    private static final Pattern SPLIT = Pattern.compile("\\s*,\\s*");

    /** Split list of trimmed tokens. */
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
