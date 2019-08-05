package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.persistence.dataset.FieldType;

import javax.ejb.Stateless;

/**
 * Factory used to construct solr fields according to given parameters.
 */
@Stateless
public class SolrFieldFactory {

    // -------------------- LOGIC --------------------

    /**
     * Constructs {@link SolrField} according to the parameters that were given.
     * <p>
     * {@link SolrField.SolrType} defaults to TEXT_EN.
     */
    public SolrField getSolrField(String datasetFieldTypeName,
                                  FieldType fieldType,
                                  boolean isMultivaluedSolrField,
                                  boolean isSolrFieldCanBeUsedAsFacetable) {

        SolrField.SolrType solrType = SolrField.SolrType.TEXT_EN;

        if (fieldType.equals(FieldType.INT)) {
            solrType = SolrField.SolrType.INTEGER;
        } else if (fieldType.equals(FieldType.FLOAT)) {
            solrType = SolrField.SolrType.FLOAT;
        } else if (fieldType.equals(FieldType.DATE)) {
            solrType = SolrField.SolrType.DATE;
        } else if (fieldType.equals(FieldType.EMAIL)) {
            solrType = SolrField.SolrType.EMAIL;
        }

        return new SolrField(datasetFieldTypeName, solrType, isMultivaluedSolrField, isSolrFieldCanBeUsedAsFacetable, true);

    }
}
