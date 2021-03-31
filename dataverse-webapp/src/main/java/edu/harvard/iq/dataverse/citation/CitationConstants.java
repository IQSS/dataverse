package edu.harvard.iq.dataverse.citation;

/**
 * Currently the constants are used mainly in RDS.
 */
public enum CitationConstants {
    DATA("citation.constants.data"),
    PRODUCER("citation.constants.producer"),
    DISTRIBUTOR("citation.constants.distributor"),
    PUBLISHER("citation.constants.publisher");

    private final String key;

    public String getKey() {
        return key;
    }

    CitationConstants(String key) {
        this.key = key;
    }
}
