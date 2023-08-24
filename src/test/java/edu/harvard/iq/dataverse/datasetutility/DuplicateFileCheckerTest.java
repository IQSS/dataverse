package edu.harvard.iq.dataverse.datasetutility;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;

public class DuplicateFileCheckerTest {

    private DuplicateFileChecker duplicateFileChecker;
    private DatasetVersionServiceBean datasetVersionServiceBean;

    @BeforeEach
    public void setUp() {
        this.datasetVersionServiceBean = mock(DatasetVersionServiceBean.class);
        this.duplicateFileChecker = new DuplicateFileChecker(datasetVersionServiceBean);
    }

    @AfterEach
    public void tearDown() {
        duplicateFileChecker = null;
    }

    // ----------------------------------------------------------------------------------------------------------
    // test constructor
    // ----------------------------------------------------------------------------------------------------------

    @Test
    void testConstructorWithUndefinedDatasetVersionService() {
        assertThrows(NullPointerException.class, () -> new DuplicateFileChecker(null));
    }

    @Test
    public void testConstructorWithDefinedDatasetVersionService() {
        DuplicateFileChecker duplicateFileChecker = new DuplicateFileChecker(new DatasetVersionServiceBean());
        assertNotNull(duplicateFileChecker);
    }

    // ----------------------------------------------------------------------------------------------------------
    // test public boolean isFileInSavedDatasetVersion(DatasetVersion datasetVersion, String checkSum)
    // ----------------------------------------------------------------------------------------------------------

    @Test
    void testIsFileInSavedDatasetVersionWithCheckSumParamWithUndefinedDatasetVersion() {
        DatasetVersion datasetVersion = null;
        String checkSum = "checkSum";
        
        assertThrows(NullPointerException.class, () -> this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, checkSum));
    }

    @Test
    void testIsFileInSavedDatasetVersionWithChecksumParamWithUndefinedChecksum() {
        DatasetVersion datasetVersion = new DatasetVersion();
        String checkSum = null;
        
        assertThrows(NullPointerException.class, () -> this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, checkSum));
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

    @Test
    void testIsFileInSavedDatasetVersionWithFileMetadataParamWithUndefinedDatasetVersion() {
        DatasetVersion datasetVersion = null;
        FileMetadata fileMetadata = new FileMetadata();
        
        assertThrows(NullPointerException.class, () -> this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, fileMetadata));
    }

    @Test
    void testIsFileInSavedDatasetVersionWithFileMetadataParamWithUndefinedFileMetadata() {
        DatasetVersion datasetVersion = new DatasetVersion();
        FileMetadata fileMetadata = null;
        
        assertThrows(NullPointerException.class, () -> this.duplicateFileChecker.isFileInSavedDatasetVersion(datasetVersion, fileMetadata));
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