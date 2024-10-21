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
        dv2.getFileMetadatas().remove(1);
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(new DatasetFieldType("Author", DatasetFieldType.FieldType.TEXT, true));
        dsf.setSingleValue("TEST");
        dv2.getDatasetFields().add(dsf);
        dv2.getFileMetadatas().get(2).setRestricted(!dv2.getFileMetadatas().get(2).isRestricted());
        DatasetVersionDifference dvd  = new DatasetVersionDifference(dv2, dv1);

        JsonObjectBuilder json = dvd.compareVersionsAsJson();
        JsonObject obj = json.build();
        System.out.println(JsonUtil.prettyPrint(obj));

        JsonPath dataFile = JsonPath.from(JsonUtil.prettyPrint(obj));
        assertTrue("TEST".equalsIgnoreCase(dataFile.getString("Metadata.Author.1")));
        assertTrue("true".equalsIgnoreCase(dataFile.getString("Files.modified[0].isRestricted.1")));
        assertTrue("disclaimer".equalsIgnoreCase(dataFile.getString("TermsOfAccess.Disclaimer.1")));
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
        dv.setTermsOfUseAndAccess(new TermsOfUseAndAccess());
        dv.setFileMetadatas(initFiles(dv));
        return dv;
    }
    private List<FileMetadata> initFiles(DatasetVersion dsv) {
        List<FileMetadata> fileMetadata = new ArrayList<>();
        for (int i=0; i < 4; i++) {
            FileMetadata fm = new FileMetadata();
            fm.setDatasetVersion(dsv);
            DataFile df = new DataFile();
            DataTable dt = new DataTable();
            dt.setOriginalFileName("filename"+i+".txt");
            df.setId(Long.valueOf(i));
            df.setDescription("Desc"+i);
            df.setRestricted(false);
            df.setFilesize(100 + i);
            df.setChecksumType(DataFile.ChecksumType.MD5);
            df.setChecksumValue("value"+i);
            df.setDataTable(dt);
            df.setOwner(dsv.getDataset());
            fm.setDataFile(df);
            fm.setLabel("Label"+i);
            fileMetadata.add(fm);
            df.setFileMetadatas(fileMetadata);

        }
        return fileMetadata;
    }
}
