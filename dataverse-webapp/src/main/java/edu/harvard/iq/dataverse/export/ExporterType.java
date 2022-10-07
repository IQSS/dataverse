package edu.harvard.iq.dataverse.export;

import java.util.Arrays;
import java.util.Optional;

public enum ExporterType {
    DDI("DDI"),
    DATACITE("DATACITE"),
    DCTERMS("DCTERMS"),
    DUBLINCORE("oai_dc"),
    JSON("dataverse_json"),
    OAIDDI("oai_ddi"),
    OAIORE("OAIORE"),
    SCHEMADOTORG("SCHEMADOTORG"),
    OPENAIRE("oai_datacite"),
    DDI_HTML("ddi_html");

    private String prefix;

    // -------------------- CONSTRUCTORS --------------------

    ExporterType(String prefix) {
        this.prefix = prefix;
    }
    // -------------------- GETTERS --------------------

    public String getPrefix() {
        return prefix;
    }

    // -------------------- LOGIC --------------------

    /**
     * @return ExporterConstant if present or Optional.empty if the enum with given string doesnt exist.
     */
    public static Optional<ExporterType> fromPrefix(String prefix) {
        return Arrays.stream(values())
                .filter(v -> v.getPrefix().equals(prefix))
                .findFirst();
    }
}
