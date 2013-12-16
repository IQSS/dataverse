package edu.harvard.iq.dataverse.api;

public class SearchFields {

    // standard fields from example/solr/collection1/conf/schema.xml
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String CATEGORY = "cat";
//    public static final String AUTHOR = "author"; // see AUTHOR_STRING not below
    // dynamic fields (for now) http://wiki.apache.org/solr/SchemaXml#Dynamic_fields
    // *_s for String
    // *_l for Long
    // etc.
    public static final String ENTITY_ID = "entityid_l";
    public static final String TYPE = "type_s";
//    public static final String AFFILIATION = "affiliation_s";
    /**
     * @todo: use a field called "author" instead. Solr default has "author" as
     * "text_general" so the field is tokenized ("Foo Bar" becomes "foo" "bar"
     * which is not what we want):
     * http://stackoverflow.com/questions/16559911/facet-query-will-give-wrong-output-on-dynamicfield-in-solr
     */
    public static final String AUTHOR_STRING = "authorstring_s";

}
