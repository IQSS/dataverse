package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataUtil;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.util.DateUtil.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.path.json.JsonPath;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class DatasetVersionDifferenceTest {

    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());

    private static List<FileMetadata> addedFiles;
    private static List<FileMetadata> removedFiles;
    private static List<FileMetadata> changedFileMetadata;
    private static List<FileMetadata> changedVariableMetadata;
    private static List<FileMetadata[]> replacedFiles;
    private static Long fileId = Long.valueOf(0);

    @BeforeAll
    public static void setUp() {
        BrandingUtilTest.setupMocks();
    }

    @AfterAll
    public static void tearDown() {
        BrandingUtilTest.setupMocks();
    }

    @Test
    public void testDifferencing() {
        Dataset dataset = new Dataset();
        License license = new License("CC0 1.0",
                "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.",
                URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"),
                true, 1l);
        license.setDefault(true);
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setTermsOfUseAndAccess(new TermsOfUseAndAccess());
        DatasetVersion datasetVersion2 = new DatasetVersion();
        datasetVersion2.setDataset(dataset);
        datasetVersion2.setVersionState(DatasetVersion.VersionState.DRAFT);

        // Published version's two files
        DataFile dataFile = new DataFile();
        dataFile.setId(1L);
        DataFile dataFile2 = new DataFile();
        dataFile2.setId(2L);

        FileMetadata fileMetadata1 = createFileMetadata(10L, datasetVersion, dataFile, "file1.txt");
        fileMetadata1.setLabel("file1.txt");

        FileMetadata fileMetadata2 = createFileMetadata(20L, datasetVersion, dataFile2, "file2.txt");

        // Draft version - same two files with one label change
        FileMetadata fileMetadata3 = fileMetadata1.createCopy();
        fileMetadata3.setId(30L);

        FileMetadata fileMetadata4 = fileMetadata2.createCopy();
        fileMetadata4.setLabel("file3.txt");
        fileMetadata4.setId(40L);

        List<FileMetadata> fileMetadatas = new ArrayList<>(Arrays.asList(fileMetadata1, fileMetadata2));
        datasetVersion.setFileMetadatas(fileMetadatas);
        List<FileMetadata> fileMetadatas2 = new ArrayList<>(Arrays.asList(fileMetadata3, fileMetadata4));
        datasetVersion2.setFileMetadatas(fileMetadatas2);

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate;
        try {
            publicationDate = dateFmt.parse("19551105");
            datasetVersion.setReleaseTime(publicationDate);
            dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<DatasetVersion> versionList = new ArrayList<>(Arrays.asList(datasetVersion, datasetVersion2));
        dataset.setVersions(versionList);

        // One file has a changed label
        List<FileMetadata> expectedAddedFiles = new ArrayList<>();
        List<FileMetadata> expectedRemovedFiles = new ArrayList<>();
        ;
        List<FileMetadata> expectedChangedFileMetadata = Arrays.asList(fileMetadata2, fileMetadata4);
        List<FileMetadata> expectedChangedVariableMetadata = new ArrayList<>();
        List<FileMetadata[]> expectedReplacedFiles = new ArrayList<>();
        List<String[]> changedTerms = new ArrayList<>();

        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);
        // change label for first file as well
        fileMetadata3.setLabel("file1_updated.txt");
        expectedChangedFileMetadata = Arrays.asList(fileMetadata1, fileMetadata3, fileMetadata2, fileMetadata4);
        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);
        // Add one change to variable metadata
        fileMetadata3.setVariableMetadatas(Arrays.asList(new VariableMetadata()));
        expectedChangedVariableMetadata = Arrays.asList(fileMetadata1, fileMetadata3);
        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);
        // Replaced File
        DataFile replacingFile = new DataFile();
        replacingFile.setId(3L);
        replacingFile.setPreviousDataFileId(1L);
        fileMetadata3.setDataFile(replacingFile);
        expectedChangedFileMetadata = Arrays.asList(fileMetadata2, fileMetadata4);
        expectedChangedVariableMetadata = new ArrayList<>();

        FileMetadata[] filePair = new FileMetadata[2];
        filePair[0] = fileMetadata1;
        filePair[1] = fileMetadata3;
        expectedReplacedFiles = new ArrayList<>();
        expectedReplacedFiles.add(filePair);
        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Add a new file
        DataFile newFile = new DataFile();
        newFile.setId(3L);
        FileMetadata fileMetadata5 = createFileMetadata(50L, datasetVersion2, newFile, "newFile.txt");
        datasetVersion2.getFileMetadatas().add(fileMetadata5);
        expectedAddedFiles = Arrays.asList(fileMetadata5);
        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Remove a file
        datasetVersion2.getFileMetadatas().remove(fileMetadata4);
        expectedRemovedFiles = Arrays.asList(fileMetadata2);
        expectedChangedFileMetadata = new ArrayList<>();
        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Set the published version's TermsOfUseAndAccess to a non-null value
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        datasetVersion.setTermsOfUseAndAccess(termsOfUseAndAccess);

        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Set the draft version's TermsOfUseAndAccess to a non-null value

        datasetVersion2.setTermsOfUseAndAccess(new TermsOfUseAndAccess());

        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Set a term field

        datasetVersion2.getTermsOfUseAndAccess().setTermsOfUse("Terms o' Use");
        String[] termField = new String[] {
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.header"), "", "Terms o' Use" };
        changedTerms.add(termField);

        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

        // Set a term field in the original version

        datasetVersion.getTermsOfUseAndAccess().setDisclaimer("Not our fault");
        String[] termField2 = new String[] {
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.disclaimer"),
                "Not our fault", "" };
        changedTerms.add(termField2);

        compareResults(datasetVersion, datasetVersion2, expectedAddedFiles, expectedRemovedFiles,
                expectedChangedFileMetadata, expectedChangedVariableMetadata, expectedReplacedFiles, changedTerms);

    }

    private FileMetadata createFileMetadata(long id, DatasetVersion datasetVersion, DataFile dataFile, String label) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(id);
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setLabel(label);
        fileMetadata.setCategories(new ArrayList<>());
        return fileMetadata;
    }

    /**
     * CompareResults is currently testing the output of the
     * DatasetVersionDifference class with the manually created expected results
     * included as parameters and with the results of the less efficient algorithm
     * it is replacing. Once we're collectively convinced that the tests here are
     * correct (i.e. the manually created expected* parameters are set correctly for
     * each use case), we could drop running the originalCalculateDifference method
     * and just compare with the expected* results.
     * 
     * @param changedTerms
     */
    private void compareResults(DatasetVersion datasetVersion, DatasetVersion datasetVersion2,
            List<FileMetadata> expectedAddedFiles, List<FileMetadata> expectedRemovedFiles,
            List<FileMetadata> expectedChangedFileMetadata, List<FileMetadata> expectedChangedVariableMetadata,
            List<FileMetadata[]> expectedReplacedFiles, List<String[]> changedTerms) {
        DatasetVersionDifference diff = new DatasetVersionDifference(datasetVersion2, datasetVersion);
        // Run the original algorithm
        originalCalculateDifference(datasetVersion2, datasetVersion);
        // Compare the old and new algorithms
        assertEquals(addedFiles, diff.getAddedFiles());
        assertEquals(removedFiles, diff.getRemovedFiles());
        assertEquals(changedFileMetadata, diff.getChangedFileMetadata());
        assertEquals(changedVariableMetadata, diff.getChangedVariableMetadata());
        assertEquals(replacedFiles.size(), diff.getReplacedFiles().size());
        for (int i = 0; i < replacedFiles.size(); i++) {
            assertEquals(replacedFiles.get(i)[0], diff.getReplacedFiles().get(i)[0]);
            assertEquals(replacedFiles.get(i)[1], diff.getReplacedFiles().get(i)[1]);
        }

        // Also compare the new algorithm with the manually created expected* values for
        // the test cases
        assertEquals(expectedAddedFiles, diff.getAddedFiles());
        assertEquals(expectedRemovedFiles, diff.getRemovedFiles());
        assertEquals(expectedChangedFileMetadata, diff.getChangedFileMetadata());
        assertEquals(expectedChangedVariableMetadata, diff.getChangedVariableMetadata());
        assertEquals(expectedReplacedFiles.size(), diff.getReplacedFiles().size());
        for (int i = 0; i < expectedReplacedFiles.size(); i++) {
            assertEquals(expectedReplacedFiles.get(i)[0], diff.getReplacedFiles().get(i)[0]);
            assertEquals(expectedReplacedFiles.get(i)[1], diff.getReplacedFiles().get(i)[1]);
        }

        assertEquals(changedTerms.size(), diff.getChangedTermsAccess().size());
        for (int i = 0; i < changedTerms.size(); i++) {
            String[] diffArray = diff.getChangedTermsAccess().get(i);
            assertEquals(changedTerms.get(i)[0], diffArray[0]);
            assertEquals(changedTerms.get(i)[1], diffArray[1]);
            assertEquals(changedTerms.get(i)[2], diffArray[2]);
        }
    }

    @Deprecated
    // This is the "Original" difference calculation from DatasetVersionDifference
    // It is included here to help verify that the new implementation is the same as
    // the original
    private static void originalCalculateDifference(DatasetVersion newVersion, DatasetVersion originalVersion) {

        addedFiles = new ArrayList<>();
        removedFiles = new ArrayList<>();
        changedFileMetadata = new ArrayList<>();
        changedVariableMetadata = new ArrayList<>();
        replacedFiles = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        // TODO: ?
        // It looks like we are going through the filemetadatas in both versions,
        // *sequentially* (i.e. at the cost of O(N*M)), to select the lists of
        // changed, deleted and added files between the 2 versions... But why
        // are we doing it, if we are doing virtually the same thing inside
        // the initDatasetFilesDifferenceList(), below - but in a more efficient
        // way (sorting both lists, then goint through them in parallel, at the
        // cost of (N+M) max.?
        // -- 4.6 Nov. 2016

        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            boolean deleted = true;
            for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    deleted = false;
                    if (!DatasetVersionDifference.compareFileMetadatas(fmdo, fmdn).isEmpty()) {
                        changedFileMetadata.add(fmdo);
                        changedFileMetadata.add(fmdn);
                    }
                    if (!VariableMetadataUtil.compareVariableMetadata(fmdo, fmdn)
                            || !DatasetVersionDifference.compareVarGroup(fmdo, fmdn)) {
                        changedVariableMetadata.add(fmdo);
                        changedVariableMetadata.add(fmdn);
                    }
                    break;
                }
            }
            if (deleted) {
                removedFiles.add(fmdo);
            }
        }
        for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
            boolean added = true;
            for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    added = false;
                    break;
                }
            }
            if (added) {
                addedFiles.add(fmdn);
            }
        }

        getReplacedFiles();
        logger.info("Difference Loop Execution time: " + (System.currentTimeMillis() - startTime) + " ms");

    }

    @Deprecated
    // This is used only in the original algorithm and was removed from
    // DatasetVersionDifference
    private static void getReplacedFiles() {
        if (addedFiles.isEmpty() || removedFiles.isEmpty()) {
            return;
        }
        List<FileMetadata> addedToReplaced = new ArrayList<>();
        List<FileMetadata> removedToReplaced = new ArrayList<>();
        for (FileMetadata added : addedFiles) {
            DataFile addedDF = added.getDataFile();
            Long replacedId = addedDF.getPreviousDataFileId();
            if (added.getDataFile().getPreviousDataFileId() != null) {
            }
            for (FileMetadata removed : removedFiles) {
                DataFile test = removed.getDataFile();
                if (test.getId().equals(replacedId)) {
                    addedToReplaced.add(added);
                    removedToReplaced.add(removed);
                    FileMetadata[] replacedArray = new FileMetadata[2];
                    replacedArray[0] = removed;
                    replacedArray[1] = added;
                    replacedFiles.add(replacedArray);
                }
            }
        }
        if (addedToReplaced.isEmpty()) {
        } else {
            addedToReplaced.stream().forEach((delete) -> {
                addedFiles.remove(delete);
            });
            removedToReplaced.stream().forEach((delete) -> {
                removedFiles.remove(delete);
            });
        }
    }

    @Test
    public void testCompareVersionsAsJson() {

        Dataverse dv = new Dataverse();
        Dataset ds = new Dataset();
        ds.setOwner(dv);
        ds.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL,"10.5072","FK2/BYM3IW", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null));

        DatasetVersion dv1 = initDatasetVersion(0L, ds, DatasetVersion.VersionState.RELEASED);
        DatasetVersion dv2 = initDatasetVersion(1L, ds, DatasetVersion.VersionState.DRAFT);
        ds.setVersions(List.of(dv1, dv2));

        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setDisclaimer("disclaimer");
        dv2.setTermsOfUseAndAccess(toa);
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(new DatasetFieldType("Author", DatasetFieldType.FieldType.TEXT, true));
        MetadataBlock mb = new MetadataBlock();
        mb.setDisplayName("testMetadataBlock");
        dsf.getDatasetFieldType().setMetadataBlock(mb);
        dsf.setSingleValue("TEST");
        dv2.getDatasetFields().add(dsf);
        // modify file at index 0
        dv2.getFileMetadatas().get(0).setRestricted(!dv2.getFileMetadatas().get(2).isRestricted());

        FileMetadata addedFile = initFile(dv2); // add a new file
        FileMetadata removedFile = dv2.getFileMetadatas().get(1); // remove the second file
        dv2.getFileMetadatas().remove(1);
        FileMetadata replacedFile = dv2.getFileMetadatas().get(1); // the third file is now at index 1 since the second file was removed
        FileMetadata replacementFile = initFile(dv2, replacedFile.getDataFile().getId()); // replace the third file with a new file
        dv2.getFileMetadatas().remove(1);

        DatasetVersionDifference dvd  = new DatasetVersionDifference(dv2, dv1);

        JsonObjectBuilder json = dvd.compareVersionsAsJson();
        JsonObject obj = json.build();
        System.out.println(JsonUtil.prettyPrint(obj));

        JsonPath dataFile = JsonPath.from(JsonUtil.prettyPrint(obj));
        assertTrue("TEST".equalsIgnoreCase(dataFile.getString("metadataChanges[0].changed[0].newValue")));
        assertTrue(addedFile.getLabel().equalsIgnoreCase(dataFile.getString("filesAdded[0].fileName")));
        assertTrue(removedFile.getLabel().equalsIgnoreCase(dataFile.getString("filesRemoved[0].fileName")));
        assertTrue(replacedFile.getLabel().equalsIgnoreCase(dataFile.getString("filesReplaced[0].oldFile.fileName")));
        assertTrue(replacementFile.getLabel().equalsIgnoreCase(dataFile.getString("filesReplaced[0].newFile.fileName")));
        assertTrue("true".equalsIgnoreCase(dataFile.getString("fileChanges[0].changed[0].newValue")));
        assertTrue("disclaimer".equalsIgnoreCase(dataFile.getString("TermsOfAccess.changed[0].newValue")));
    }
    
    @Test
    public void testGetSummaryAsJson(){
                
        Dataverse dv = new Dataverse();
        Dataset ds = new Dataset();
        ds.setOwner(dv);
        ds.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL,"10.5072","FK2/S1E7K3", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null));

        DatasetVersion dv1 = initDatasetVersion(0L, ds, DatasetVersion.VersionState.RELEASED);
        dv1.setId(Long.valueOf(1));
        DatasetVersion dv2 = initDatasetVersion(1L, ds, DatasetVersion.VersionState.DRAFT);
        dv2.setId(Long.valueOf(2));
        ds.setVersions(List.of(dv2, dv1));

        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setDisclaimer("disclaimer");
        dv2.setTermsOfUseAndAccess(toa);
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(new DatasetFieldType("Author", DatasetFieldType.FieldType.TEXT, true));
        MetadataBlock mb = new MetadataBlock();
        mb.setDisplayName("testMetadataBlock");
        mb.setName("testMetadataBlock");
        dsf.getDatasetFieldType().setMetadataBlock(mb);
        dsf.setSingleValue("TEST");
        dv2.getDatasetFields().add(dsf);
        // modify file at index 0
        dv2.getFileMetadatas().get(0).setRestricted(!dv2.getFileMetadatas().get(2).isRestricted());

        FileMetadata addedFile = initFile(dv2); // add a new file
        FileMetadata removedFile = dv2.getFileMetadatas().get(1); // remove the second file
        dv2.getFileMetadatas().remove(1);
        FileMetadata replacedFile = dv2.getFileMetadatas().get(1); // the third file is now at index 1 since the second file was removed
        FileMetadata replacementFile = initFile(dv2, replacedFile.getDataFile().getId()); // replace the third file with a new file
        dv2.getFileMetadatas().remove(1);

        DatasetVersionDifference dvd  = dv2.getDefaultVersionDifference();
        


        JsonObjectBuilder json = dvd.getSummaryDifferenceAsJson();
        JsonObject obj = json.build();
        JsonPath dataFile = JsonPath.from(JsonUtil.prettyPrint(obj));
                
        assertTrue("true".equalsIgnoreCase(dataFile.getString("termsAccessChanged")));
        assertEquals(2,(Long.parseLong(dataFile.getString("files.changedFileMetaData"))));
        assertEquals(0,(Long.parseLong(dataFile.getString("testMetadataBlock.deleted"))));
        assertEquals(1, (int) (Long.parseLong(dataFile.getString("testMetadataBlock.added"))));
        assertEquals(1,(Long.parseLong(dataFile.getString("files.added"))));

        
    }
    
    
    private DatasetVersion initDatasetVersion(Long id, Dataset ds, DatasetVersion.VersionState vs) {
        DatasetVersion dv = new DatasetVersion();
        dv.setDataset(ds);
        dv.setVersion(1L);
        dv.setVersionState(vs);
        dv.setMinorVersionNumber(0L);
        if (vs == DatasetVersion.VersionState.RELEASED) {
            dv.setVersionNumber(1L);
            dv.setVersion(1L);
            dv.setReleaseTime(now());
        }
        dv.setId(id);
        dv.setCreateTime(now());
        dv.setLastUpdateTime(now());
        dv.setTermsOfUseAndAccess(new TermsOfUseAndAccess());
        dv.setFileMetadatas(initFiles(dv));
        return dv;
    }
    private List<FileMetadata> initFiles(DatasetVersion dsv) {
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileId = 0L;
        for (int i=0; i < 10; i++) {
            FileMetadata fm = initFile(dsv);
            fileMetadatas.add(fm);
        }
        return fileMetadatas;
    }
    private FileMetadata initFile(DatasetVersion dsv) {
        return initFile(dsv, null);
    }
    private FileMetadata initFile(DatasetVersion dsv, Long prevId) {
        Long id = fileId++;
        FileMetadata fm = new FileMetadata();
        DataFile df = new DataFile();
        fm.setDatasetVersion(dsv);
        DataTable dt = new DataTable();
        dt.setOriginalFileName("filename"+id+".txt");
        df.setId(id);
        df.setDescription("Desc"+id);
        df.setRestricted(false);
        df.setFilesize(100 + id);
        df.setChecksumType(DataFile.ChecksumType.MD5);
        df.setChecksumValue("value"+id);
        df.setDataTable(dt);
        df.setOwner(dsv.getDataset());
        df.getFileMetadatas().add(fm);
        df.setPreviousDataFileId(prevId);
        fm.setId(id);
        fm.setDataFile(df);
        fm.setLabel("Label"+id);
        fm.setDirectoryLabel("/myFilePath/");
        fm.setDescription("Desc"+id);
        dsv.getFileMetadatas().add(fm);
        return fm;
    }
}
