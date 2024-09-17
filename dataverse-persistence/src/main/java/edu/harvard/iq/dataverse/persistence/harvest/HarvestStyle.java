package edu.harvard.iq.dataverse.persistence.harvest;

/**
 * Different harvesting "styles". These define how we format and
 * display metadata harvested from various remote resources.
 */
public enum HarvestStyle {
    DATAVERSE("Dataverse v4+"),
    VDC("DVN, v2-3"),
    ICPSR("ICPSR"),
    NESSTAR("Nesstar archive"),
    ROPER("Roper Archive"),
    HGL("HGL"),
    DOI("DOI"),
    DEFAULT("Generic OAI resource (DC)");

    final String description;

    HarvestStyle(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
