package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;

public class DataverseThemeTest {
    private DataverseTheme dataverseTheme;

    @BeforeEach
    public void before() {
        this.dataverseTheme = new DataverseTheme();
        this.dataverseTheme.setId(1L);
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.dataverseTheme.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        DatasetFieldType datasetFieldType = new DatasetFieldType();

        assertFalse(this.dataverseTheme.equals(datasetFieldType));
    }

    @Test
    public void testEqualsWithSameClassSameId() {
        DataverseTheme dataverseTheme1 = new DataverseTheme();
        dataverseTheme1.setId(1L);

        assertTrue(this.dataverseTheme.equals(dataverseTheme1));
    }

    @Test
    public void testEqualsWithSameClassDifferentId() {
        DataverseTheme dataverseTheme1 = new DataverseTheme();
        dataverseTheme1.setId(2L);

        assertFalse(this.dataverseTheme.equals(dataverseTheme1));
    }
}