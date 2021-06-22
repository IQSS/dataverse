package edu.harvard.iq.dataverse.search.ror;


enum RorSolrField {

    ID("id"),
    ROR_ID("rorId"),
    NAME("name"),
    COUNTRY_NAME("countryName"),
    COUNTRY_CODE("countryCode"),
    CITY("city"),
    WEBSITE("website"),
    NAME_ALIAS("nameAlias"),
    ACRONYM("acronym"),
    LABEL("label");

    private final String exactName;

    RorSolrField(String exactName) {
        this.exactName = exactName;
    }

    String getExactName() {
        return exactName;
    }
}
