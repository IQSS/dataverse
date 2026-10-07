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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import org.dataverse.unf.UNFUtil;
import org.dataverse.unf.UnfException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);

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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);
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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);

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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);
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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);
        assertFalse(file3NameAltered);

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
        assertTrue(file1NameAltered);
        assertTrue(file2NameAltered);
        assertTrue(file3NameAltered);
    }

    @Test
    /**
     * Test adding a file with a full path duplicating an existing directory
     * or with an ancestor that duplicates the full path of an existing file.
     */
    public void testCheckFilesDuplicatingDirectories() throws Exception {

        class Params {
            final int iteration;
            final FileMetadata fmd;

            public Params(int iteration, String dir, String fileLabel) {
                this.iteration = iteration;

                var datafile = new DataFile("application/octet-stream");

                fmd = new FileMetadata();
                fmd.setLabel(fileLabel);
                fmd.setDirectoryLabel(dir);
                fmd.setDataFile(datafile);
                datafile.getFileMetadatas().add(fmd);
            }
        }
        // each iteration adds one or more files to the dataset and
        // verifies what names would be added if all would be added (again)
        // the adjusted names are used to add files in the next iteration
        var paramsList = Arrays.asList(
            new Params(0, "foo","bar"),
            new Params(1, null, "foo"), // file/dir conflict: "foo"
            new Params(1, null, "bar"),
            new Params(2, null, "bar"),
            new Params(3, "bar/foo","pint"), // dir/file conflict: "bar"
            new Params(4, "bar/foo/pint", "beer")  // dir-ancestor/file conflict: "bar/foo/pint"
        );
        // more than 10 List.of elements cause subtle type problems for the assertions
        var expectedFilesInDataset = Arrays.asList(
            List.of("foo/bar"),
            List.of("foo/bar", "null/foo-1", "null/bar"),
            List.of("foo/bar", "null/foo-1", "null/bar", "null/bar-2"),
            List.of("foo/bar", "null/foo-1", "null/bar", "null/bar-2", "bar/foo/pint"),
            List.of("foo/bar", "null/foo-1", "null/bar", "null/bar-2", "bar/foo/pint", "bar/foo/pint/beer")
        );
        var expectedLabelsAfterTest = Arrays.asList(
            List.of( "foo/bar-1", "null/foo-1", "null/bar", "null/bar-1", "bar/foo/pint", "bar/foo/pint/beer"),
            List.of( "foo/bar-1", "null/foo-2", "null/bar-1", "null/bar-2", "bar/foo/pint", "bar/foo/pint/beer"),
            List.of( "foo/bar-1", "null/foo-2", "null/bar-1", "null/bar-3", "bar/foo/pint", "bar/foo/pint/beer"),
            List.of( "foo/bar-1", "null/foo-2", "null/bar-1", "null/bar-3", "bar/foo/pint-1", "bar/foo/pint/beer"),
            List.of( "foo/bar-1", "null/foo-2", "null/bar-1", "null/bar-3", "bar/foo/pint-1", "bar/foo/pint/beer-1")
        );
        List<List<String>> expectedNewDataFilesAfterTest = Arrays.asList(
            List.of( "foo/bar-1", "null/foo-1", "null/bar","null/bar-1", "bar/foo/pint", "bar/foo/pint/beer"),
            List.of(),
            List.of(), // ???
            List.of(),
            List.of()
        );

        // create dataset version
        var dataset = makeDataset();
        var datasetVersion = dataset.getLatestVersion();
        datasetVersion.setFileMetadatas(new ArrayList<>());

        for (int i=0; i<expectedFilesInDataset.size(); i++) {
            var newDataFiles = new ArrayList<DataFile>();
            paramsList.forEach(p -> newDataFiles.add(p.fmd.getDataFile()));
            // select files to add to the dataset for the current iteration
            var ii = i;
            var filesToAdd = paramsList.stream()
                .filter(p -> p.iteration == ii)
                .map(p -> p.fmd).toList();
            // add files to dataset
            for (var fmd: filesToAdd) {
                    var fmdClone = new FileMetadata();
                    fmdClone.setId(MocksFactory.nextId());
                    fmdClone.setLabel(fmd.getLabel());
                    fmdClone.setDirectoryLabel(fmd.getDirectoryLabel());
                    fmdClone.setDatasetVersion(fmd.getDatasetVersion());
                    if (fmd.getDataFile() != null) {
                        var df = fmd.getDataFile();
                        var dfClone = new DataFile(df.getContentType());
                        fmdClone.setDataFile(dfClone);
                        dfClone.getFileMetadatas().add(fmdClone);
                    }
                    datasetVersion.getFileMetadatas().add(fmdClone);
                    fmdClone.setDatasetVersion(datasetVersion);
            }

            // precondition
            var actualFilesInDataset = datasetVersion.getFileMetadatas().stream()
                .map(fmd -> fmd.getDirectoryLabel() + "/" + fmd.getLabel()).toList();
            assertThat(actualFilesInDataset)
                .withFailMessage("expectedFilesInDataset %d \n  expected %s \n  but got  %s", i, expectedFilesInDataset.get(i), actualFilesInDataset)
                .containsExactlyInAnyOrderElementsOf(expectedFilesInDataset.get(i));

            // method under test
            IngestUtil.checkForDuplicateFileNamesFinal(datasetVersion, newDataFiles, null);

            // postconditions
            var actualPaths = paramsList.stream()
                .map(p -> p.fmd.getDirectoryLabel() + "/" + p.fmd.getLabel()).toList();
            assertThat(actualPaths)
                .withFailMessage("expectedLabelsAfterTest %d \n  expected %s \n  but got  %s", i, expectedLabelsAfterTest.get(i), actualPaths)
                .containsExactlyInAnyOrderElementsOf(expectedLabelsAfterTest.get(i));
            var actualNewDataFiles = newDataFiles.stream()
                .map(p -> p.getFileMetadata().getDirectoryLabel() + "/" + p.getFileMetadata().getLabel()).toList();
            assertThat(actualNewDataFiles)
                .withFailMessage("expectedNewDataFilesAfterTest %d \n  expected %s \n  but got  %s", i, expectedNewDataFilesAfterTest.get(i), actualNewDataFiles)
                .containsExactlyInAnyOrderElementsOf(expectedNewDataFilesAfterTest.get(i));

        }
    }

    @Test
    /**
     * Test adding files to a dataset having a file with a full path duplicating a directory.
     */
    public void testExistingFilesDuplicatingDirectories() throws Exception {

        // create dataset version
        var dataset = makeDataset();
        var datasetVersion = dataset.getLatestVersion();
        datasetVersion.setFileMetadatas(new ArrayList<>());

        // add files to dataset
        Stream.of(
            Arrays.asList("foo","bar"),
            Arrays.asList(null, "foo"), // file/dir conflict: "foo"
            Arrays.asList(null, "bar"),
            Arrays.asList("bar/foo","pint"), // dir/file conflict: "bar"
            Arrays.asList("bar/foo/pint", "beer")  // subdir/file conflict: "bar/foo/pint"
        ).forEach(l -> {
            var dir = l.get(0);
            var fileLabel = l.get(1);
            var datafile = new DataFile("application/octet-stream");
            var fmd = new FileMetadata();
            fmd.setId(MocksFactory.nextId());
            fmd.setLabel(fileLabel);
            fmd.setDirectoryLabel(dir);
            fmd.setDataFile(datafile);
            datafile.getFileMetadatas().add(fmd);

            // add file to dataset
            datasetVersion.getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(datasetVersion);
        });

        // EditDataFilesPage.save() would create an error message if result is not empty
        var duplicates = IngestUtil.findDuplicateFilenames(datasetVersion, List.of());

        assertThat(duplicates).containsExactlyInAnyOrderElementsOf(List.of("bar", "foo", "bar/foo/pint"));
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
        assertTrue(file2NameAltered);
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
        assertTrue(file1NameAltered);
        assertFalse(file2NameAltered);
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
        assertTrue(datafile1.isTabularData());

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
        assertFalse(IngestUtil.shouldHaveUnf(null));
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
        assertThat(IngestUtil.findConflictingPathPart(pathPlusFilename, fileMetadatas))
            .hasValue("README.md");
    }

    @Test
    public void addDirConflictingWithFile() {
        FileMetadata fmd = new FileMetadata();
        fmd.setDirectoryLabel("foo");
        fmd.setLabel("bar");
        var fileMetadatas = Arrays.asList(fmd);
        assertThat(IngestUtil.findConflictingPathPart("foo/bar/pint", fileMetadatas))
            .hasValue("foo/bar");
    }

    @Test
    public void addParentDirConflictingWithFile() {
        FileMetadata fmd = new FileMetadata();
        fmd.setLabel("foo");
        var fileMetadatas = Arrays.asList(fmd);
        assertThat(IngestUtil.findConflictingPathPart("foo/bar/pint", fileMetadatas))
            .hasValue("foo");
    }

    @Test
    public void noConflict() {
        FileMetadata fmd = new FileMetadata();
        fmd.setLabel("foo");
        var fileMetadatas = Arrays.asList(fmd);
        assertThat(IngestUtil.findConflictingPathPart("bar", fileMetadatas))
            .isEmpty();
    }

}
