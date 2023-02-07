package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.persistence.dataset.FieldType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SolrField {

    private static Map<SolrType, DynamicFieldData> SOLR_TYPE_TO_DYNAMIC_FIELD_DATA = Initializer.initSolrTypeToDynamicFieldData();
    private static Map<FieldType, SolrType> FIELD_TYPE_TO_SOLR_TYPE = Initializer.initFieldTypeToSolrType();

    private String nameSearchable;
    private String nameFacetable;
    private SolrType solrType;
    private boolean allowedToBeMultivalued;
    private boolean facetable;

    // -------------------- CONSTRUCTORS --------------------

    public SolrField(String name, SolrType solrType, boolean allowedToBeMultivalued, boolean facetable, boolean isDatasetField) {

        this.facetable = facetable;
        this.solrType = solrType;
        if (isDatasetField) {
            // prefixes for Solr dynamicField's specified in schema.xml
            DynamicFieldData fieldData = SOLR_TYPE_TO_DYNAMIC_FIELD_DATA.get(solrType);
            if (fieldData != null) {
                nameSearchable = fieldData.prefix.getPrefix() + name;
                this.facetable = this.facetable && fieldData.facetable;
            } else {
                nameSearchable = name;
            }
            this.allowedToBeMultivalued = true; // by default, dynamicFields are multivalued
            this.nameFacetable = name + "_ss";
        } else {
            this.nameSearchable = name;
            this.nameFacetable = allowedToBeMultivalued ? name + "_ss" : name + "_s";
            this.allowedToBeMultivalued = allowedToBeMultivalued;
        }
    }

    // -------------------- GETTERS --------------------

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

    public boolean isFacetable() {
        return facetable;
    }

    // -------------------- LOGIC --------------------

    public static SolrField of(String datasetFieldTypeName, FieldType fieldType, boolean isMultivaluedSolrField,
                               boolean isSolrFieldCanBeUsedAsFacetable) {
        SolrType solrType = FIELD_TYPE_TO_SOLR_TYPE.getOrDefault(fieldType, SolrType.TEXT_EN);
        return new SolrField(datasetFieldTypeName, solrType, isMultivaluedSolrField, isSolrFieldCanBeUsedAsFacetable, true);
    }

    // -------------------- SETTERS --------------------

    public void setAllowedToBeMultivalued(boolean allowedToBeMultivalued) {
        this.allowedToBeMultivalued = allowedToBeMultivalued;
    }

    // -------------------- INNER CLASSES --------------------

    public enum SolrType {

        // @todo: make this configurable from text_en to text_general or non-English languages? We changed it to text_en
        // to improve English language searching in https://github.com/IQSS/dataverse/issues/444 We want to get away
        // from always using "text_en" (especially to support range queries) in https://github.com/IQSS/dataverse/issues/370

        STRING("string"),
        TEXT_EN("text_en"),
        INTEGER("pint"),
        LONG("plong"),
        DATE("text_en"),
        EMAIL("text_en"),
        FLOAT("pfloat"),
        GEOBOX("geobox");

        private String type;

        SolrType(String string) {
            type = string;
        }

        public String getType() {
            return type;
        }
    }

    private static class DynamicFieldData {
        public final SearchDynamicFieldPrefix prefix;
        public final boolean facetable;

        public DynamicFieldData(SearchDynamicFieldPrefix prefix, boolean facetable) {
            this.prefix = prefix;
            this.facetable = facetable;
        }
    }

    private static class Initializer {
        public static Map<SolrType, DynamicFieldData> initSolrTypeToDynamicFieldData() {
            Map<SolrType, DynamicFieldData> index = new HashMap<>();
            index.put(SolrType.STRING, new DynamicFieldData(SearchDynamicFieldPrefix.STRING, true));
            index.put(SolrType.TEXT_EN, new DynamicFieldData(SearchDynamicFieldPrefix.TEXT, true));
            index.put(SolrType.EMAIL, new DynamicFieldData(SearchDynamicFieldPrefix.TEXT, true));
            index.put(SolrType.DATE, new DynamicFieldData(SearchDynamicFieldPrefix.DATE, true));
            index.put(SolrType.INTEGER, new DynamicFieldData(SearchDynamicFieldPrefix.INTEGER, true));
            index.put(SolrType.FLOAT, new DynamicFieldData(SearchDynamicFieldPrefix.FLOAT, false));
            index.put(SolrType.GEOBOX, new DynamicFieldData(SearchDynamicFieldPrefix.GEOBOX, true));
            return Collections.unmodifiableMap(index);
        }

        public static Map<FieldType, SolrType> initFieldTypeToSolrType() {
            Map<FieldType, SolrType> index = new HashMap<>();
            index.put(FieldType.INT, SolrType.INTEGER);
            index.put(FieldType.FLOAT, SolrType.FLOAT);
            index.put(FieldType.DATE, SolrType.DATE);
            index.put(FieldType.EMAIL, SolrType.EMAIL);
            index.put(FieldType.GEOBOX, SolrType.GEOBOX);
            return Collections.unmodifiableMap(index);
        }
    }
}
