package edu.harvard.iq.dataverse.search;

import java.util.stream.Stream;

public enum SearchDynamicFieldPrefix {
    TEXT("dsf_txt_"),
    STRING("dsf_str_"),
    DATE("dsf_dtr_"),
    INTEGER("dsf_int_"),
    FLOAT("dsf_flt_"),
    GEOBOX("dsf_geobox_");


    private String prefix;

// -------------------- CONSTRUCTORS --------------------
    SearchDynamicFieldPrefix(String prefix) {
        this.prefix = prefix;
    }

// -------------------- GETTERS --------------------
    public String getPrefix() {
        return prefix;
    }

// -------------------- LOGIC --------------------
    public static boolean contains(String value) {
        return Stream.of(SearchDynamicFieldPrefix.values())
                .anyMatch(prefix -> prefix.getPrefix().equals(value));
    }
}
