package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataset;

/**
 * Tests against IngestServiceBean helper methods.
 * 
 * @author bmckinney 
 */
public class IngestServiceBeanHelperTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    /**
     * Test adding duplicate file name labels to a dataset version with no subdirectories.
     */
    public void testCheckForDuplicateFileNamesNoDirectories() throws Exception {
        
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getEditVersion();
        datasetVersion.setCreateTime( dateFmt.parse("20001012") );
        datasetVersion.setLastUpdateTime( datasetVersion.getLastUpdateTime() );
        datasetVersion.setId( MocksFactory.nextId() );
        datasetVersion.setReleaseTime( dateFmt.parse("20010101") );
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
        
        IngestServiceBeanHelper.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList);
        
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
        assertEquals(file1NameAltered, true);
        assertEquals(file2NameAltered, true);
        
        // try to add data files with "-1" duplicates and see if it gets incremented to "-2"
        IngestServiceBeanHelper.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList);

        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("datafile1-2.txt")) {
                file1NameAltered = true;
            }
            if (df.getFileMetadata().getLabel().equals("datafile2-2.txt")) {
                file2NameAltered = true;
            }
        }

        // check filenames are unique and unaltered
        assertEquals(file1NameAltered, true);
        assertEquals(file2NameAltered, true);
    }

    @Test
    /**
     * Test adding duplicate file name labels with directories, including a duplicate file name label in another 
     * directory.
     */
    public void testCheckForDuplicateFileNamesWithDirectories() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getEditVersion();
        datasetVersion.setCreateTime( dateFmt.parse("20001012") );
        datasetVersion.setLastUpdateTime( datasetVersion.getLastUpdateTime() );
        datasetVersion.setId( MocksFactory.nextId() );
        datasetVersion.setReleaseTime( dateFmt.parse("20010101") );
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

        IngestServiceBeanHelper.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList);

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
        assertEquals(file1NameAltered, true);
        assertEquals(file2NameAltered, true);
        assertEquals(file3NameAltered, false);

        // add duplicate file in root
        datasetVersion.getFileMetadatas().add(fmd3);
        fmd3.setDatasetVersion(datasetVersion);
        
        // try to add data files with "-1" duplicates and see if it gets incremented to "-2"
        IngestServiceBeanHelper.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList);

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
        assertEquals(file1NameAltered, true);
        assertEquals(file2NameAltered, true);
        assertEquals(file3NameAltered, true);
    }

    @Test
    /**
     * Test tabular files (e.g., .dta) are changed when .tab files with the same name exist.
     */
    public void testCheckForDuplicateFileNamesTabular() throws Exception {

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

        // create dataset
        Dataset dataset = makeDataset();

        // create dataset version
        DatasetVersion datasetVersion = dataset.getEditVersion();
        datasetVersion.setCreateTime( dateFmt.parse("20001012") );
        datasetVersion.setLastUpdateTime( datasetVersion.getLastUpdateTime() );
        datasetVersion.setId( MocksFactory.nextId() );
        datasetVersion.setReleaseTime( dateFmt.parse("20010101") );
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

        IngestServiceBeanHelper.checkForDuplicateFileNamesFinal(datasetVersion, dataFileList);
        
        boolean file2NameAltered = false;
        for (DataFile df : dataFileList) {
            if (df.getFileMetadata().getLabel().equals("foobar-1.dta")) {
                file2NameAltered = true;
            }
        }

        // check filename is altered since tabular and will change to .tab after ingest
        assertEquals(file2NameAltered, true);
    }

}
