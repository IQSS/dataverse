package edu.harvard.iq.dataverse.common.files.mime;

public enum MimePrefix {

    AUDIO("audio"),
    CODE("code"),
    DOCUMENT("document"),
    ASTRO("astro"),
    IMAGE("image"),
    NETWORK("network"),
    GEO("geodata"),
    TABULAR("tabular"),
    VIDEO("video"),
    PACKAGE("package"),
    OTHER("other");

    private String prefixValue;

    MimePrefix(String prefix) {
        this.prefixValue = prefix;
    }

    public String getPrefixValue() {
        return prefixValue;
    }
}
