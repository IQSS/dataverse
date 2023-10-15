package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;

public class DatasetFieldDefaultValueTest {
    private DatasetFieldDefaultValue dataverseContact;

    @BeforeEach
    public void before() {
        this.dataverseContact = new DatasetFieldDefaultValue();
        this.dataverseContact.setId(1L);
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.dataverseContact.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        DatasetField datasetField = new DatasetField();

        assertFalse(this.dataverseContact.equals(datasetField));
    }

    @Test
    public void testEqualsWithSameClassSameId() {
        DatasetFieldDefaultValue dataverseContact1 = new DatasetFieldDefaultValue();
        dataverseContact1.setId(1L);

        assertTrue(this.dataverseContact.equals(dataverseContact1));
    }

    @Test
    public void testEqualsWithSameClassDifferentId() {
        DatasetFieldDefaultValue dataverseContact1 = new DatasetFieldDefaultValue();
        dataverseContact1.setId(2L);

        assertFalse(this.dataverseContact.equals(dataverseContact1));
    }
}