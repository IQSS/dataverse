package edu.harvard.iq.dataverse.export;

import java.util.Optional;

public enum ExporterType {

    DDI,
    DATACITE,
    DCTERMS,
    DUBLINCORE,
    JSON,
    OAIDDI,
    OAIORE,
    SCHEMADOTORG,
    OPENAIRE;

    /**
     * @return ExporterConstant if present or Optional.empty if the enum with given string doesnt exist.
     */
    public static Optional<ExporterType> fromString(String enumValue) {

        try {
            return Optional.of(ExporterType.valueOf(enumValue.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
