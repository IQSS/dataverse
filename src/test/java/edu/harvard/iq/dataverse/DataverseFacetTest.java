package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;

public class DataverseFacetTest {
    private DataverseFacet dataverseFacet;

    @BeforeEach
    public void before() {
        this.dataverseFacet = new DataverseFacet();
        this.dataverseFacet.setId(1L);
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.dataverseFacet.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        DatasetFieldType datasetFieldType = new DatasetFieldType();

        assertFalse(this.dataverseFacet.equals(datasetFieldType));
    }

    @Test
    public void testEqualsWithSameClassSameId() {
        DataverseFacet dataverseFacet1 = new DataverseFacet();
        dataverseFacet1.setId(1L);

        assertTrue(this.dataverseFacet.equals(dataverseFacet1));
    }

    @Test
    public void testEqualsWithSameClassDifferentId() {
        DataverseFacet dataverseFacet1 = new DataverseFacet();
        dataverseFacet1.setId(2L);

        assertFalse(this.dataverseFacet.equals(dataverseFacet1));
    }
}