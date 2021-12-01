package edu.harvard.iq.dataverse.api.imports;

/**
 * @author ellenk
 */
public enum ImportType {
    /**
     * TODO: had to do a distinction because of otherMath tag causing problem, will be discussing about it in pull request
     **/
    IMPORT,

    /**
     * Data is harvested from another Dataverse instance
     */
    HARVEST
}