package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

public class UrlTokenUtilTest {

    @Test
    public void testGetToolUrlWithOptionalQueryParameters() {

        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        FileMetadata fmd = new FileMetadata();
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        ds.setId(50L);
        ds.setGlobalId(new GlobalId("doi:10.5072/FK2ABCDEF"));
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        URLTokenUtil urlTokenUtil = new URLTokenUtil(dataFile, apiToken, fmd, "en");
        assertEquals("en", urlTokenUtil.replaceTokensWithValues("{localeCode}"));
        assertEquals("42 test en", urlTokenUtil.replaceTokensWithValues("{fileId} test {localeCode}"));
        assertEquals("42 test en", urlTokenUtil.replaceTokensWithValues("{fileId} test {localeCode}"));
        
        assertEquals("https://librascholar.org/api/files/42/metadata?key=" + apiToken.getTokenString(), urlTokenUtil.replaceTokensWithValues("{siteUrl}/api/files/{fileId}/metadata?key={apiToken}"));
        
        URLTokenUtil urlTokenUtil2 = new URLTokenUtil(ds, apiToken, "en");
        assertEquals("https://librascholar.org/api/datasets/50?key=" + apiToken.getTokenString(), urlTokenUtil2.replaceTokensWithValues("{siteUrl}/api/datasets/{datasetId}?key={apiToken}"));
        assertEquals("https://librascholar.org/api/datasets/:persistentId/?persistentId=doi:10.5072/FK2ABCDEF&key=" + apiToken.getTokenString(), urlTokenUtil2.replaceTokensWithValues("{siteUrl}/api/datasets/:persistentId/?persistentId={datasetPid}&key={apiToken}"));
    }
}
