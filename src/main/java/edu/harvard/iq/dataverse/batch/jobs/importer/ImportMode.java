package edu.harvard.iq.dataverse.batch.jobs.importer;

/**
 * <code>ImportMode</code> is used to define how importing content is applied
 * to the existing content in the repository.
 */
public enum ImportMode {

    /**
     * Default behavior. Existing content is not modified, i.e. only new content is added and
     * none is deleted or modified.
     */
    MERGE,

    /**
     * Existing content is updated, new content is added and none is deleted.
     */
    UPDATE,
    
    /**
     * Existing content is replaced completely by the imported
     * content, i.e. is overridden or deleted accordingly.
     */
    REPLACE
    
}