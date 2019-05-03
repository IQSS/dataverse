package edu.harvard.iq.dataverse.datasetutility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;

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

    // ----------------------------------------------------------------------------------------------------------
    // test constructor
    // ----------------------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testConstructorWithUndefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(null);
    }

    @Test
    public void testConstructorWithDefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(new DatasetVersionServiceBean());
        assertNotNull(duplicateFileChecker);
    }

    // ----------------------------------------------------------------------------------------------------------
    // test public boolean isFileInSavedDatasetVersion(DatasetVersion datasetVersion, String checkSum)
    // ----------------------------------------------------------------------------------------------------------

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

    // ----------------------------------------------------------------------------------------------------------
    // test public boolean isFileInSavedDatasetVersion(DatasetVersion datasetVersion, FileMetadata fileMetadata)
    // ----------------------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testIsFileInSavedDatasetVersionWithFileMetadataParamWithUndefinedDatasetVersion() {
        DatasetVersion datasetVersion = null;
        FileMetadata fileMetadata = new FileMetadata();

        this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, fileMetadata);
    }

    @Test(expected = NullPointerException.class)
    public void testIsFileInSavedDatasetVersionWithFileMetadataParamWithUndefinedFileMetadata() {
        DatasetVersion datasetVersion = new DatasetVersion();
        FileMetadata fileMetadata = null;

        this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, fileMetadata);
    }

    @Test
    public void testIsFileInSavedDatasetVersionWithFileMetadataParamWithUnsavedFile() {
        DatasetVersion datasetVersion = new DatasetVersion();
        FileMetadata fileMetadata = new FileMetadata();
        String checkSum = "checkSum";
        fileMetadata.setDataFile(new DataFile());
        fileMetadata.getDataFile().setChecksumValue(checkSum);

        // mock sql query
        Mockito.when(this.datasetVersionServiceBean.doesChecksumExistInDatasetVersion(datasetVersion, checkSum)).thenReturn(false);

        assertFalse(this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, fileMetadata));
    }

}