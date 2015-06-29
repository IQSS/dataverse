package edu.harvard.iq.dataverse.search;

public class SearchConstants {

    // these are the values we index into Solr as "dvtype"
    public static final String DATAVERSES = "dataverses";
    public static final String DATASETS = "datasets";
    public static final String FILES = "files";

    // these are the values we want the user to operate on from the Search API
    public static final String DATAVERSE = "dataverse";
    public static final String DATASET = "dataset";
    public static final String FILE = "file";
    
    // Params used for returning search JSON
    // See example: http://guides.dataverse.org/en/latest/api/search.html
    //
    public static final String SEARCH_API_TOTAL_COUNT = "total_count";
    public static final String SEARCH_API_START = "start";
    public static final String SEARCH_API_ITEMS = "items";

}
