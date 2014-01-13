package edu.harvard.iq.dataverse.api;

public class SearchFields {

    // standard fields from example/solr/collection1/conf/schema.xml
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String CATEGORY = "cat";
    // i.e. http://pdurbin.pagekite.me/search.xhtml?q=*&fq0=citationyear_i%3A[2001+TO+2007]
    public static final String CITATION_YEAR = "citationyear_i";
//    public static final String AUTHOR = "author"; // see AUTHOR_STRING not below
    // dynamic fields (for now) http://wiki.apache.org/solr/SchemaXml#Dynamic_fields
    // *_s for String
    // *_ss for multivalued String
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
    public static final String FILE_TYPE = "filetype_s";
    public static final String FILE_TYPE_GROUP = "filetypegroup_s";
    public static final String ORIGINAL_DATAVERSE = "originaldataverse_s";
    public static final String SUBTREE = "subtree_ss";
    // i.e. http://localhost:8080/search.xhtml?q=*&fq0=citationdate_dt:[2008-01-01T00%3A00%3A00Z+TO+2011-01-01T00%3A00%3A00Z%2B1YEAR}
    public static final String CITATION_DATE = "citationdate_dt";

}
