package edu.harvard.iq.dataverse.search;

import java.util.stream.Stream;

public enum SearchDynamicFieldPrefix {
    TXT ("dsf_txt_"),
    STR ("dsf_str_"),
    DTR ("dsf_dtr_"),
    INT ("dsf_int_"),
    FLT ("dsf_flt_")
    ;


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
