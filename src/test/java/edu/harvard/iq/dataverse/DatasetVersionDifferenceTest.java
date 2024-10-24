package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.path.json.JsonPath;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.util.DateUtil.now;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetVersionDifferenceTest {

    static Long fileId = Long.valueOf(0);
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
