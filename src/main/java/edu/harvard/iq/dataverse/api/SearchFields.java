package edu.harvard.iq.dataverse.api;

public class SearchFields {

    // standard fields from example/solr/collection1/conf/schema.xml
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String AUTHOR = "author";
    // dynamic fields (for now) http://wiki.apache.org/solr/SchemaXml#Dynamic_fields
    // *_s for String
    // *_l for Long
    // etc.
    public static final String ENTITY_ID = "entityid_l";
    public static final String TYPE = "type_s";
    public static final String AFFILIATION = "affiliation_s";

}
