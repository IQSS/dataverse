package edu.harvard.iq.dataverse.search;

public class SolrField {

    private String nameSearchable;
    private String nameFacetable;
    private SolrType solrType;
    private boolean allowedToBeMultivalued;
    private boolean facetable;


    /**
     *
     * @param name
     * @param solrType
     * @param allowedToBeMultivalued
     * @param facetable
     * @param isDatasetField
     */
    public SolrField(String name, SolrType solrType, boolean allowedToBeMultivalued, boolean facetable, boolean isDatasetField) {

        this.facetable = facetable;
        if(isDatasetField) {
            /**
             * prefixes for Solr dynamicField's specified in schema.xml
             */
            if (solrType.equals(SolrType.STRING)) {
                nameSearchable = "dsf_str_" + name;
                nameFacetable = "dsf_str_" + name;
            } else if (solrType.equals(SolrType.TEXT_EN) ||
                    solrType.equals(SolrType.EMAIL) ||
                    solrType.equals(SolrType.DATE)) {
                nameSearchable = "dsf_txt_" + name;
                nameFacetable = "dsf_txt_" + name;
            } else if (solrType.equals(SolrType.INTEGER)) {
                nameSearchable = "dsf_int_" + name;
                nameFacetable = "dsf_int_" + name;
            } else if (solrType.equals(SolrType.FLOAT)) {
                nameSearchable = "dsf_flt_" + name;
                nameFacetable = name;
                this.facetable = false;
            }
            else {
                nameSearchable = name;
                nameFacetable = name;
            }

            this.allowedToBeMultivalued = true; // by default, dynamicFields are multivalued
        } else {
            this.nameSearchable = name;
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

            this.allowedToBeMultivalued = allowedToBeMultivalued;
        }

        this.solrType = solrType;
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
        STRING("string"), TEXT_EN("text_en"),
        INTEGER("pint"), LONG("plong"),
        DATE("text_en"), EMAIL("text_en"), FLOAT("pfloat");

        private String type;

        private SolrType(String string) {
            type = string;
        }

        public String getType() {
            return type;
        }

    }

}
