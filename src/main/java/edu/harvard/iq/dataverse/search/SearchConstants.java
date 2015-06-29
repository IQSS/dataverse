package edu.harvard.iq.dataverse.search;

public class SearchConstants {

    // these are the values we index into Solr as "dvtype"
    public static final String DATAVERSES = "dataverses";
    public static final String DATASETS = "datasets";
    public static final String FILES = "files";

    // Used in solr queries, as in "(dvobject_types:(Dataverse OR Dataset))"
    public static final String SOLR_DVOBJECT_TYPES = "dvObjectType";
    public static final String SOLR_PUBLICATION_STATUSES = "publicationStatus";
        
    // these are the values we want the user to operate on from the Search API
    public static final String DATAVERSE = "dataverse";
    public static final String DATASET = "dataset";
    public static final String FILE = "file";
    
    

}
