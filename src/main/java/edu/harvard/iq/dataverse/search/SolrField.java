package edu.harvard.iq.dataverse.search;

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
            /**
             * @todo Should we expose this Solr-specific "_ss" fieldname here?
             * Instead should the field end with "FacetMultiple"?
             */
            this.nameFacetable = name + "_ss";
        } else {
            /**
             * @todo Should we expose this Solr-specific "_s" fieldname here?
             * Instead should the field end with "FacetSingle"?
             */
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

        /**
         * @todo: make this configurable from text_en to text_general or
         * non-English languages? We changed it to text_en to improve English
         * language searching in https://github.com/IQSS/dataverse/issues/444
         *
         * We want to get away from always using "text_en" (especially to
         * support range queries) in
         * https://github.com/IQSS/dataverse/issues/370
         */
        STRING("string"), TEXT_EN("text_en"), INTEGER("plong"), FLOAT("pdouble"), DATE("date_range"), EMAIL("text_en");

        private String type;

        private SolrType(String string) {
            type = string;
        }

        public String getType() {
            return type;
        }

    }

}
