package edu.harvard.iq.dataverse.search.query;

import java.util.Arrays;

/**
 * Enum containing all possible publication statuses that
 * can be assigned to dvObject in search.
 * <p>
 * Some of publication statuses can be overlapped
 * (single dvObject can have more than one status).
 * For example (dvObject with DRAFT status can be in IN_REVIEW status)
 * 
 * @author madryk
 */
public enum SearchPublicationStatus {
    
    PUBLISHED("Published"),
    UNPUBLISHED("Unpublished"),
    DRAFT("Draft"),
    IN_REVIEW("In_Review"),
    DEACCESSIONED("Deaccessioned");
    
    private String solrValue;
    
    // -------------------- CONSTRUCTORS --------------------
    
    private SearchPublicationStatus(String solrValue) {
        this.solrValue = solrValue;
    }

    // -------------------- GETTERS --------------------
    
    /**
     * Returns value of field that is stored in solr index.
     */
    public String getSolrValue() {
        return solrValue;
    }
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns enum based on value from solr index.
     */
    public static SearchPublicationStatus fromSolrValue(String solrValue) {
        return valueOf(solrValue.toUpperCase());
    }
}