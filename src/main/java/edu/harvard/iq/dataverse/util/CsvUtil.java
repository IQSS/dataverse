package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lightweight helpers for simple comma separated configuration values (NOT full
 * RFC 4180 parsing).
 * Intended for admin-entered settings where tokens are simple identifiers
 * (origins, methods, headers, etc.).
 */
public final class CsvUtil {
    /** Shared split regex allowing arbitrary surrounding whitespace. */
    public static final String COMMA_BETWEEN_OPTIONAL_WHITE_SPACE = "\\s*,\\s*";
    private static final Pattern COMMA_WS = Pattern.compile(COMMA_BETWEEN_OPTIONAL_WHITE_SPACE);

    private CsvUtil() {
    }

    /** Strip outer quotes, remove remaining quotes, trim. */
    public static String sanitize(String raw) {
        if (raw == null)
            return null;
        String v = raw.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\"", "").trim();
    }

    /** Split into an ordered immutable list of sanitized non-empty tokens. */
    public static List<String> split(String rawCsv) {
        String sanitized = sanitize(rawCsv);
        if (sanitized == null || sanitized.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(COMMA_WS.split(sanitized))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(CsvUtil::sanitize)
                .collect(Collectors.toUnmodifiableList());
    }

    /** Split into a de-duplicated (in insertion order) set of sanitized tokens. */
    public static Set<String> splitToSet(String rawCsv) {
        List<String> list = split(rawCsv);
        if (list.isEmpty())
            return Collections.emptySet();
        return list.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Canonical normalized CSV string (tokens joined by ', '). */
    public static String normalize(String rawCsv) {
        List<String> list = split(rawCsv);
        if (list.isEmpty())
            return "";
        return String.join(", ", list);
    }

    /** Convenience: split into a lowercase insertion-ordered set (dedup, case-fold). */
    public static Set<String> splitToLowerCaseSet(String rawCsv) {
        if (rawCsv == null || rawCsv.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return split(rawCsv).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
