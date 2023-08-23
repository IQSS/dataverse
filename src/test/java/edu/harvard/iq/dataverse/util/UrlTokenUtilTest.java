package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlTokenUtilTest {

    @Test
    @JvmSetting(key = JvmSettings.SITE_URL, value = "https://librascholar.org")
    void testGetToolUrlWithOptionalQueryParameters() {
        // given
        String siteUrl = "https://librascholar.org";
        
        DataFile dataFile = new DataFile();
        dataFile.setId(42L);
        FileMetadata fmd = new FileMetadata();
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        ds.setId(50L);
        ds.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL,"10.5072","FK2ABCDEF",null, DOIServiceBean.DOI_RESOLVER_URL, null));
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
    
        // when & then 1/2
        URLTokenUtil urlTokenUtil = new URLTokenUtil(dataFile, apiToken, fmd, "en");
        assertEquals("en", urlTokenUtil.replaceTokensWithValues("{localeCode}"));
        assertEquals("42 test en", urlTokenUtil.replaceTokensWithValues("{fileId} test {localeCode}"));
        assertEquals("42 test en", urlTokenUtil.replaceTokensWithValues("{fileId} test {localeCode}"));
        assertEquals( siteUrl + "/api/files/42/metadata?key=" + apiToken.getTokenString(),
            urlTokenUtil.replaceTokensWithValues("{siteUrl}/api/files/{fileId}/metadata?key={apiToken}"));
    
        // when & then 2/2
        URLTokenUtil urlTokenUtil2 = new URLTokenUtil(ds, apiToken, "en");
        assertEquals(siteUrl + "/api/datasets/50?key=" + apiToken.getTokenString(),
            urlTokenUtil2.replaceTokensWithValues("{siteUrl}/api/datasets/{datasetId}?key={apiToken}"));
        assertEquals(siteUrl + "/api/datasets/:persistentId/?persistentId=doi:10.5072/FK2ABCDEF&key=" + apiToken.getTokenString(),
            urlTokenUtil2.replaceTokensWithValues("{siteUrl}/api/datasets/:persistentId/?persistentId={datasetPid}&key={apiToken}"));
    }
}
