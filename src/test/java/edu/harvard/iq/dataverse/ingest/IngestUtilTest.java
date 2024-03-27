package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataset;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.validation.ConstraintViolation;
import org.dataverse.unf.UNFUtil;
import org.dataverse.unf.UnfException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IngestUtilTest {

    String logFile = "/tmp/testLogFile";

    @Test
    /**
     * Test adding duplicate file name labels to a dataset version with no
     * subdirectories.
     */
    public void testCheckForDuplicateFileNamesNoDirectories() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setCreateTime(dateFmt.parse("20001012"));
        datasetVersion.setLastUpdateTime(datasetVersion.getLastUpdateTime());
        datasetVersion.setId(MocksFactory.nextId());
        datasetVersion.setReleaseTime(dateFmt.parse("20010101"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setMinorVersionNumber(0L);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // create datafiles
        List<DataFile> dataFileList = new ArrayList<>();
        DataFile datafile1 = new DataFile("application/octet-stream");
        datafile1.setStorageIdentifier("datafile1.txt");
        datafile1.setFilesize(200);
        datafile1.setModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setCreateDate(new Timestamp(new Date().getTime()));
        datafile1.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setOwner(dataset);
        datafile1.setIngestDone();
        datafile1.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile1.setChecksumValue("Unknown");

        // set metadata and add verson
        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("datafile1.txt");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        datasetVersion.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile1);

        DataFile datafile2 = new DataFile("application/octet-stream");
        datafile2.setStorageIdentifier("datafile2.txt");
        datafile2.setFilesize(200);
        datafile2.setModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setCreateDate(new Timestamp(new Date().getTime()));
        datafile2.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setOwner(dataset);
        datafile2.setIngestDone();
        datafile2.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile2.setChecksumValue("Unknown");

        // set metadata and add version
        FileMetadata fmd2 = new FileMetadata();
        fmd2.setId(2L);
        fmd2.setLabel("datafile2.txt");
        fmd2.setDataFile(datafile2);
        datafile2.getFileMetadatas().add(fmd2);
        datasetVersion.getFileMetadatas().add(fmd2);
        fmd2.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile2);

        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        boolean file1NameAltered = false;
        boolean file2NameAltered = false;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-1.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-1.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and altered
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);

        // try to add data files with "-1" duplicates and see if it gets incremented to "-2"
        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-2.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-2.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and altered
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);
    }

    @Test
    /**
     * Test adding duplicate file name labels to a dataset version with empty
     * directory labels. This should simulate what happens when uploading a file
     * via the file upload UI.
     */
    public void testCheckForDuplicateFileNamesWithEmptyDirectoryLabels() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setCreateTime(dateFmt.parse("20001012"));
        datasetVersion.setLastUpdateTime(datasetVersion.getLastUpdateTime());
        datasetVersion.setId(MocksFactory.nextId());
        datasetVersion.setReleaseTime(dateFmt.parse("20010101"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setMinorVersionNumber(0L);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // create datafiles
        List<DataFile> dataFileList = new ArrayList<>();
        DataFile datafile1 = new DataFile("application/octet-stream");
        datafile1.setStorageIdentifier("datafile1.txt");
        datafile1.setFilesize(200);
        datafile1.setModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setCreateDate(new Timestamp(new Date().getTime()));
        datafile1.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setOwner(dataset);
        datafile1.setIngestDone();
        datafile1.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile1.setChecksumValue("Unknown");

        // set metadata and add verson
        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("datafile1.txt");
        fmd1.setDirectoryLabel("");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        datasetVersion.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile1);

        DataFile datafile2 = new DataFile("application/octet-stream");
        datafile2.setStorageIdentifier("datafile2.txt");
        datafile2.setFilesize(200);
        datafile2.setModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setCreateDate(new Timestamp(new Date().getTime()));
        datafile2.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setOwner(dataset);
        datafile2.setIngestDone();
        datafile2.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile2.setChecksumValue("Unknown");

        // set metadata and add version
        FileMetadata fmd2 = new FileMetadata();
        fmd2.setId(2L);
        fmd2.setLabel("datafile2.txt");
        fmd2.setDirectoryLabel("");
        fmd2.setDataFile(datafile2);
        datafile2.getFileMetadatas().add(fmd2);
        datasetVersion.getFileMetadatas().add(fmd2);
        fmd2.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile2);

        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        boolean file1NameAltered = false;
        boolean file2NameAltered = false;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-1.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-1.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and altered
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);

        // try to add data files with "-1" duplicates and see if it gets incremented to "-2"
        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-2.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-2.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and altered
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);
    }

    @Test
    /**
     * Test adding duplicate file name labels with directories, including a
     * duplicate file name label in another directory.
     */
    public void testCheckForDuplicateFileNamesWithDirectories() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setCreateTime(dateFmt.parse("20001012"));
        datasetVersion.setLastUpdateTime(datasetVersion.getLastUpdateTime());
        datasetVersion.setId(MocksFactory.nextId());
        datasetVersion.setReleaseTime(dateFmt.parse("20010101"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setMinorVersionNumber(0L);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // create datafiles
        List<DataFile> dataFileList = new ArrayList<>();
        DataFile datafile1 = new DataFile("application/octet-stream");
        datafile1.setStorageIdentifier("subdir/datafile1.txt");
        datafile1.setFilesize(200);
        datafile1.setModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setCreateDate(new Timestamp(new Date().getTime()));
        datafile1.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setOwner(dataset);
        datafile1.setIngestDone();
        datafile1.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile1.setChecksumValue("Unknown");

        // set metadata and add version
        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("datafile1.txt");
        fmd1.setDirectoryLabel("subdir");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        datasetVersion.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile1);

        DataFile datafile2 = new DataFile("application/octet-stream");
        datafile2.setStorageIdentifier("subdir/datafile2.txt");
        datafile2.setFilesize(200);
        datafile2.setModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setCreateDate(new Timestamp(new Date().getTime()));
        datafile2.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setOwner(dataset);
        datafile2.setIngestDone();
        datafile2.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile2.setChecksumValue("Unknown");

        // set metadata and add version
        FileMetadata fmd2 = new FileMetadata();
        fmd2.setId(2L);
        fmd2.setLabel("datafile2.txt");
        fmd2.setDirectoryLabel("subdir");
        fmd2.setDataFile(datafile2);
        datafile2.getFileMetadatas().add(fmd2);
        datasetVersion.getFileMetadatas().add(fmd2);
        fmd2.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile2);

        DataFile datafile3 = new DataFile("application/octet-stream");
        datafile3.setStorageIdentifier("datafile2.txt");
        datafile3.setFilesize(200);
        datafile3.setModificationTime(new Timestamp(new Date().getTime()));
        datafile3.setCreateDate(new Timestamp(new Date().getTime()));
        datafile3.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile3.setOwner(dataset);
        datafile3.setIngestDone();
        datafile3.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile3.setChecksumValue("Unknown");

        // set metadata and add version
        FileMetadata fmd3 = new FileMetadata();
        fmd3.setId(3L);
        fmd3.setLabel("datafile2.txt");
        fmd3.setDataFile(datafile3);
        datafile3.getFileMetadatas().add(fmd3);

        dataFileList.add(datafile3);

        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        boolean file1NameAltered = false;
        boolean file2NameAltered = false;
        boolean file3NameAltered = true;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-1.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-1.txt")) {
                file2NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2.txt")) {
                file3NameAltered = false;
            }
        }

        // check filenames are unique
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);
        assertEquals(false, file3NameAltered);

        // add duplicate file in root
        datasetVersion.getFileMetadatas().add(fmd3);
        fmd3.setDatasetVersion(datasetVersion);

        // try to add data files with "-1" duplicates and see if it gets incremented to "-2"
        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-2.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-2.txt")) {
                file2NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-1.txt")) {
                file3NameAltered = true;
            }
        }

        // check filenames are unique
        assertEquals(true, file1NameAltered);
        assertEquals(true, file2NameAltered);
        assertEquals(true, file3NameAltered);
    }

    @Test
    /**
     * Test tabular files (e.g., .dta) are changed when .tab files with the same
     * name exist.
     */
    public void testCheckForDuplicateFileNamesTabular() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setCreateTime(dateFmt.parse("20001012"));
        datasetVersion.setLastUpdateTime(datasetVersion.getLastUpdateTime());
        datasetVersion.setId(MocksFactory.nextId());
        datasetVersion.setReleaseTime(dateFmt.parse("20010101"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setMinorVersionNumber(0L);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // create datafiles
        List<DataFile> dataFileList = new ArrayList<>();
        DataFile datafile1 = new DataFile("application/x-strata");
        datafile1.setStorageIdentifier("foobar.dta");
        datafile1.setFilesize(200);
        datafile1.setModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setCreateDate(new Timestamp(new Date().getTime()));
        datafile1.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setOwner(dataset);
        datafile1.setIngestDone();
        datafile1.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile1.setChecksumValue("Unknown");
        DataTable dt1 = new DataTable();
        dt1.setOriginalFileFormat("application/x-stata");
        datafile1.setDataTable(dt1);

        // set metadata and add version
        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("foobar.tab");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        datasetVersion.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(datasetVersion);

        DataFile datafile2 = new DataFile("application/x-strata");
        datafile2.setStorageIdentifier("foobar.dta");
        datafile2.setFilesize(200);
        datafile2.setModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setCreateDate(new Timestamp(new Date().getTime()));
        datafile2.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setOwner(dataset);
        datafile2.setIngestDone();
        datafile2.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile2.setChecksumValue("Unknown");
        DataTable dt2 = new DataTable();
        dt2.setOriginalFileFormat("application/x-stata");
        datafile2.setDataTable(dt2);

        // set metadata and add version
        FileMetadata fmd2 = new FileMetadata();
        fmd2.setId(2L);
        fmd2.setLabel("foobar.dta");
        fmd2.setDataFile(datafile2);
        datafile2.getFileMetadatas().add(fmd2);

        dataFileList.add(datafile2);

        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, null);

        boolean file2NameAltered = false;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("foobar-1.dta")) {
                file2NameAltered = true;
            }
        }

        // check filename is altered since tabular and will change to .tab after ingest
        assertEquals(true, file2NameAltered);
    }

    
    @Test
    /**
     * Test adding duplicate file name labels to a dataset version with empty
     * directory labels when replacing a file. This should simulate what happens when replacing a file
     * via the file upload UI.
     */
    public void testCheckForDuplicateFileNamesWhenReplacing() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setCreateTime(dateFmt.parse("20001012"));
        datasetVersion.setLastUpdateTime(datasetVersion.getLastUpdateTime());
        datasetVersion.setId(MocksFactory.nextId());
        datasetVersion.setReleaseTime(dateFmt.parse("20010101"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setMinorVersionNumber(0L);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // create datafiles
        List<DataFile> dataFileList = new ArrayList<>();
        DataFile datafile1 = new DataFile("application/octet-stream");
        datafile1.setStorageIdentifier("datafile1.txt");
        datafile1.setFilesize(200);
        datafile1.setModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setCreateDate(new Timestamp(new Date().getTime()));
        datafile1.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile1.setOwner(dataset);
        datafile1.setIngestDone();
        datafile1.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile1.setChecksumValue("Unknown");
        datafile1.setId(1L);

        // set metadata and add verson
        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("datafile1.txt");
        fmd1.setDirectoryLabel("");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        datasetVersion.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile1);

        DataFile datafile2 = new DataFile("application/octet-stream");
        datafile2.setStorageIdentifier("datafile2.txt");
        datafile2.setFilesize(200);
        datafile2.setModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setCreateDate(new Timestamp(new Date().getTime()));
        datafile2.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile2.setOwner(dataset);
        datafile2.setIngestDone();
        datafile2.setChecksumType(DataFile.ChecksumType.SHA1);
        datafile2.setChecksumValue("Unknown");
        datafile2.setId(2L);

        // set metadata and add version
        FileMetadata fmd2 = new FileMetadata();
        fmd2.setId(2L);
        fmd2.setLabel("datafile2.txt");
        fmd2.setDirectoryLabel("");
        fmd2.setDataFile(datafile2);
        datafile2.getFileMetadatas().add(fmd2);
        datasetVersion.getFileMetadatas().add(fmd2);
        fmd2.setDatasetVersion(datasetVersion);

        dataFileList.add(datafile2);

        /*In a real replace, there should only be one file in dataFileList. Having both files in dataFileList, we're essentially testing two cases at once:
         *  - the replacing file name conflicts with some other file's name
         *  - the replacing file's name only conflicts with the file being replaced (datafile2) and shouldn't be changed
         */
        IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList, datafile2);

        boolean file1NameAltered = false;
        boolean file2NameAltered = false;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-1.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-1.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and unaltered
        assertEquals(true, file1NameAltered);
        assertEquals(false, file2NameAltered);
    }
    
    @Test
    public void testDirectoryLabels() {

        DatasetVersion datasetVersion = new DatasetVersion();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("foo.png");
        fileMetadata.setDirectoryLabel("/has/leading/slash");
        datasetVersion.getFileMetadatas().add(fileMetadata);

        //We are programmatically stripping of leading and trailing slashes
        Set<ConstraintViolation> violations1 = datasetVersion.validate();
        assertEquals(0, violations1.size());


        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations2 = datasetVersion.validate();
        assertEquals(0, violations2.size());

        fileMetadata.setDirectoryLabel("has/trailing/slash/");
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations3 = datasetVersion.validate();
        assertEquals(0, violations3.size());
        
        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations4 = datasetVersion.validate();
        assertEquals(0, violations4.size());

        fileMetadata.setDirectoryLabel("just/right");
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations5 = datasetVersion.validate();
        assertEquals(0, violations5.size());

        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations6 = datasetVersion.validate();
        assertEquals(0, violations6.size());

        fileMetadata.setDirectoryLabel("");
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations7 = datasetVersion.validate();
        assertEquals(0, violations7.size());

        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations8 = datasetVersion.validate();
        assertEquals(0, violations8.size());

        fileMetadata.setDirectoryLabel(null);
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations9 = datasetVersion.validate();
        assertEquals(0, violations9.size());

        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations10 = datasetVersion.validate();
        assertEquals(0, violations10.size());

        String singleCharacter = "a";
        fileMetadata.setDirectoryLabel(singleCharacter);
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations11 = datasetVersion.validate();
        assertEquals(0, violations11.size());

        // reset
        datasetVersion.setFileMetadatas(new ArrayList<>());
        Set<ConstraintViolation> violations12 = datasetVersion.validate();
        assertEquals(0, violations12.size());

        fileMetadata.setDirectoryLabel("/leadingAndTrailing/");
        datasetVersion.getFileMetadatas().add(fileMetadata);
        Set<ConstraintViolation> violations13 = datasetVersion.validate();
        assertEquals(0, violations13.size());

    }

    @Test
    public void testRecalculateDatasetVersionUNF() {
        IngestUtil.recalculateDatasetVersionUNF(null);
        DatasetVersion dsvNoFile = new DatasetVersion();
        IngestUtil.recalculateDatasetVersionUNF(dsvNoFile);
        assertEquals(null, dsvNoFile.getUNF());

        List<Dataset> datasets = new ArrayList<>();
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("fakeAuthority");
        dataset.setIdentifier("12345");
        DatasetVersion dsv1 = new DatasetVersion();
        dsv1.setDataset(dataset);
        dsv1.setId(42l);
        dsv1.setVersionState(DatasetVersion.VersionState.DRAFT);
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        datasetVersions.add(dsv1);

        DataFile datafile1 = new DataFile("application/octet-stream");
        DataTable dataTable = new DataTable();
        dataTable.setUnf("unfOnDataTable");
        datafile1.setDataTable(dataTable);
        assertEquals(true, datafile1.isTabularData());

        FileMetadata fmd1 = new FileMetadata();
        fmd1.setId(1L);
        fmd1.setLabel("datafile1.txt");
        fmd1.setDataFile(datafile1);
        datafile1.getFileMetadatas().add(fmd1);
        dsv1.getFileMetadatas().add(fmd1);
        fmd1.setDatasetVersion(dsv1);

        dataset.setVersions(datasetVersions);
        datasets.add(dataset);

        assertEquals(null, dsv1.getUNF());
        IngestUtil.recalculateDatasetVersionUNF(dsv1);
        assertEquals("UNF:6:rDlgOhoEkEQQdwtLRHjmtw==", dsv1.getUNF());

    }

    @Test
    public void testShouldHaveUnf() {
        DatasetVersion dsv1 = new DatasetVersion();
        IngestUtil.recalculateDatasetVersionUNF(dsv1);
        assertEquals(null, dsv1.getUNF());
    }

    @Test
    public void testGetUnfValuesOfFiles() {
        List<String> emptyList = new ArrayList<>();
        assertEquals(emptyList, IngestUtil.getUnfValuesOfFiles(null));

    }

    @Test
    public void testshouldHaveUnf() {
        assertEquals(false, IngestUtil.shouldHaveUnf(null));
    }

    @Test
    public void testUnfUtil() {
        String[] unfValues = {"a", "b", "c"};
        String datasetUnfValue = null;
        try {
            datasetUnfValue = UNFUtil.calculateUNF(unfValues);
        } catch (IOException ex) {
            Logger.getLogger(IngestUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnfException ex) {
            Logger.getLogger(IngestUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals("UNF:6:FWBO/a1GcxDnM3fNLdzrHw==", datasetUnfValue);
    }

    @Test
    public void testPathPlusFilename() {
        String incomingLabel = "incomingLabel";
        String incomingDirectoryLabel = "incomingDirectoryLabel";
        String existingLabel = "existingLabel";
        String existingDirectoryLabel = "existingDirectoryLabel";
        String pathPlusFilename = IngestUtil.getPathAndFileNameToCheck(incomingLabel, incomingDirectoryLabel, existingLabel, existingDirectoryLabel);
        assertEquals("incomingDirectoryLabel/incomingLabel", pathPlusFilename);
    }

    @Test
    public void renameFileToSameName() {
        String pathPlusFilename = "README.md";
        FileMetadata file1 = new FileMetadata();
        file1.setLabel("README.md");
        FileMetadata file2 = new FileMetadata();
        file2.setLabel("README2.md");
        List<FileMetadata> fileMetadatas = Arrays.asList(file1, file2);
        assertTrue(IngestUtil.conflictsWithExistingFilenames(pathPlusFilename, fileMetadatas));
    }

}
