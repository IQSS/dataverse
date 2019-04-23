package edu.harvard.iq.dataverse.datasetutility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;

public class DuplicateFileCheckerTest {

    private DuplicateFileChecker duplicateFileChecker;
    private DatasetVersionServiceBean datasetVersionServiceBean;

    @Before
    public void setUp() {
        this.datasetVersionServiceBean = mock(DatasetVersionServiceBean.class);
        this.duplicateFileChecker = new DuplicateFileChecker(datasetVersionServiceBean);
    }

    @After
    public void tearDown() {
        duplicateFileChecker = null;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithUndefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(null);
    }

    @Test
    public void testConstructorWithDefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(new DatasetVersionServiceBean());
        assertNotNull(duplicateFileChecker);
    }

    @Test(expected = NullPointerException.class)
    public void testIsFileInSavedDatasetVersionWithCheckSumParamWithUndefinedDatasetVersion() {
        DatasetVersion datasetVersion = null;
        String checkSum = "checkSum";

        this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, checkSum);
    }

    @Test(expected = NullPointerException.class)
    public void testIsFileInSavedDatasetVersionWithChecksumParamWithUndefinedChecksum() {
        DatasetVersion datasetVersion = new DatasetVersion();
        String checkSum = null;

        this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, checkSum);
    }

    @Test
    public void testIsFileInSavedDatasetVersionWithChecksumParamWithUnsavedFile() {
        DatasetVersion datasetVersion = new DatasetVersion();
        String checkSum = "checkSum";

        // mock sql query
        Mockito.when(this.datasetVersionServiceBean.doesChecksumExistInDatasetVersion(datasetVersion, checkSum))
                .thenReturn(false);

        assertFalse(this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, checkSum));
    }

}