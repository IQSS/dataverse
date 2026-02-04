package edu.harvard.iq.dataverse.search;

public class SearchConstants {

    // these are the values we index into Solr as "dvtype"
    public static final String DATAVERSES = "dataverses";
    public static final String SOLR_DATAVERSES = DATAVERSES;  // transitional, see #2490

    public static final String DATASETS = "datasets";
    public static final String SOLR_DATASETS = DATASETS;  // transitional, see #2490

    public static final String FILES = "files";
    public static final String SOLR_FILES = FILES;  // transitional, see #2490



    // these are the values we want the user to operate on from the Search API
    public static final String DATAVERSE = "dataverse";
    public static final String DATASET = "dataset";
    public static final String FILE = "file";
    

    // these are the values we show in the web UI as facets
    public static final String UI_DATAVERSES = "Dataverses";
    public static final String UI_DATASETS = "Datasets";
    public static final String UI_FILES = "Files";
    
// Params used for returning search JSON
    // See example: http://guides.dataverse.org/en/latest/api/search.html
    //
    public static final String SEARCH_API_TOTAL_COUNT = "total_count";
    public static final String SEARCH_API_START = "start";
    public static final String SEARCH_API_ITEMS = "items";
    
    // Number of SOLR rows/docs to retrieve
    //
    public static final int NUM_SOLR_DOCS_TO_RETRIEVE = 10;

    public static final String PUBLIC = "Public";
    public static final String RESTRICTED = "Restricted";
    public static final String EMBARGOEDTHENRESTRICTED = "EmbargoedThenRestricted";
    public static final String EMBARGOEDTHENPUBLIC = "EmbargoedThenPublic";
    public static final String RETENTIONEXPIRED = "RetentionPeriodExpired";
}
