package edu.harvard.iq.dataverse.datasetutility;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean;

public class DuplicateFileCheckerTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorWithUndefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(null);
    }

    @Test
    public void testConstructorWithDefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(new DatasetVersionServiceBean());
        assertNotNull(duplicateFileChecker);
    }

}