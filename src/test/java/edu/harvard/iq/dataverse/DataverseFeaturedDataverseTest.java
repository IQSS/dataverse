package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;

public class DataverseFeaturedDataverseTest {
    private DataverseFeaturedDataverse dataverseFeaturedDataverse;

    @BeforeEach
    public void before() {
        this.dataverseFeaturedDataverse = new DataverseFeaturedDataverse();
        this.dataverseFeaturedDataverse.setId(1L);
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.dataverseFeaturedDataverse.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        DatasetFieldType datasetFieldType = new DatasetFieldType();

        assertFalse(this.dataverseFeaturedDataverse.equals(datasetFieldType));
    }

    @Test
    public void testEqualsWithSameClassSameId() {
        DataverseFeaturedDataverse dataverseFeaturedDataverse1 = new DataverseFeaturedDataverse();
        dataverseFeaturedDataverse1.setId(1L);

        assertTrue(this.dataverseFeaturedDataverse.equals(dataverseFeaturedDataverse1));
    }

    @Test
    public void testEqualsWithSameClassDifferentId() {
        DataverseFeaturedDataverse dataverseFeaturedDataverse1 = new DataverseFeaturedDataverse();
        dataverseFeaturedDataverse1.setId(2L);

        assertFalse(this.dataverseFeaturedDataverse.equals(dataverseFeaturedDataverse1));
    }
}