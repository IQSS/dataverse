package edu.harvard.iq.dataverse;

public class SolrField {

    private String nameSearchable;
    private String nameFacetable;
    private SolrType solrType;
    private boolean allowedToBeMultivalued;
    private boolean facetable;

    public SolrField(String name, SolrType solrType, boolean allowedToBeMultivalued, boolean facetable) {
        this.nameSearchable = name;
        this.solrType = solrType;
        this.allowedToBeMultivalued = allowedToBeMultivalued;
        this.facetable = facetable;
        if (allowedToBeMultivalued) {
            this.nameFacetable = name + "_ss";
        } else {
            this.nameFacetable = name + "_s";
        }
    }

    public String getNameSearchable() {
        return nameSearchable;
    }

    public String getNameFacetable() {
        return nameFacetable;
    }

    public SolrType getSolrType() {
        return solrType;
    }

    public Boolean isAllowedToBeMultivalued() {
        return allowedToBeMultivalued;
    }

    public void setAllowedToBeMultivalued(boolean allowedToBeMultivalued) {
        this.allowedToBeMultivalued = allowedToBeMultivalued;
    }

    public boolean isFacetable() {
        return facetable;
    }

    public enum SolrType {

        STRING("string"), TEXT_GENERAL("text_general"), INTEGER("int"), LONG("long");

        private String type;

        private SolrType(String string) {
            type = string;
        }

        public String getType() {
            return type;
        }

    }

}
