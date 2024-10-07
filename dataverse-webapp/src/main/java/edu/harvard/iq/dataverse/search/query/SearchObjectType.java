package edu.harvard.iq.dataverse.search.query;

/**
 * Enum containing all possible object types that
 * can be assigned to dvObject in search.
 * 
 * @author madryk
 */
public enum SearchObjectType {
    
    DATAVERSES("dataverses"),
    DATASETS("datasets"),
    FILES("files");
    
    private final String solrValue;
    
    // -------------------- CONSTRUCTORS --------------------
    
    private SearchObjectType(final String solrValue) {
        
        this.solrValue = solrValue;
    }

    // -------------------- GETTERS --------------------
    
    /**
     * Returns value of field that is stored in solr index.
     */
    public String getSolrValue() {
        
        return this.solrValue;
    }
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns enum based on value from solr index.
     */
    public static SearchObjectType fromSolrValue(final String solrValue) {
        
        return valueOf(solrValue.toUpperCase());
    }
}