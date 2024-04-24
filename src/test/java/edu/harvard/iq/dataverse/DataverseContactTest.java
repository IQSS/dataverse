package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;

public class DataverseContactTest {
    private DataverseContact dataverseContact;

    @BeforeEach
    public void before() {
        this.dataverseContact = new DataverseContact();
        this.dataverseContact.setId(1L);
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.dataverseContact.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        DatasetFieldType datasetFieldType = new DatasetFieldType();

        assertFalse(this.dataverseContact.equals(datasetFieldType));
    }

    @Test
    public void testEqualsWithSameClassSameId() {
        DataverseContact dataverseContact1 = new DataverseContact();
        dataverseContact1.setId(1L);

        assertTrue(this.dataverseContact.equals(dataverseContact1));
    }

    @Test
    public void testEqualsWithSameClassDifferentId() {
        DataverseContact dataverseContact1 = new DataverseContact();
        dataverseContact1.setId(2L);

        assertFalse(this.dataverseContact.equals(dataverseContact1));
    }
}