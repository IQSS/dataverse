package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class SolrFieldTest {

    private final String FIELD_NAME = "test";

    // -------------------- TESTS --------------------

    @Test
    public void of_forIntType() {

        //when
        SolrField solrField = SolrField.of(FIELD_NAME, FieldType.INT, true, false);

        //then
        Assert.assertEquals(SolrField.SolrType.INTEGER, solrField.getSolrType());
        Assert.assertTrue(solrField.getNameSearchable().contains(FIELD_NAME));
        Assert.assertTrue(solrField.isAllowedToBeMultivalued());
        Assert.assertFalse(solrField.isFacetable());
    }

    @Test
    public void of_forDateType() {

        //when
        SolrField solrField = SolrField.of(FIELD_NAME, FieldType.DATE, true, false);

        //then
        Assert.assertEquals(SolrField.SolrType.DATE, solrField.getSolrType());
        Assert.assertTrue(solrField.getNameSearchable().contains(FIELD_NAME));
        Assert.assertTrue(solrField.isAllowedToBeMultivalued());
        Assert.assertFalse(solrField.isFacetable());
    }

    @Test
    public void of_forURLType() {

        //when
        SolrField solrField = SolrField.of(FIELD_NAME, FieldType.URL, true, false);

        //then
        Assert.assertEquals(SolrField.SolrType.TEXT_EN, solrField.getSolrType());
        Assert.assertTrue(solrField.getNameSearchable().contains(FIELD_NAME));
        Assert.assertTrue(solrField.isAllowedToBeMultivalued());
        Assert.assertFalse(solrField.isFacetable());
    }
}