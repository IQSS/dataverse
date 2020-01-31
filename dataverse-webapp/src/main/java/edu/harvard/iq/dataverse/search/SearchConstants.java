package edu.harvard.iq.dataverse.search;

public class SearchConstants {
    
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

}
