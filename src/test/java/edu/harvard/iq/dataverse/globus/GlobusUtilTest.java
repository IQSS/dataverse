package edu.harvard.iq.dataverse.globus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.dataaccess.AbstractRemoteOverlayAccessIO;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.GlobusAccessibleStore;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;

public class GlobusUtilTest {

    private Dataset dataset;
    private DataFile mDatafile;
    private DataFile rDatafile;
    private String baseStoreId1 = "182ad2bda2f-c3508e719076";
    private String baseStoreId2 = "182ad2bda2f-c3508e719077";
    private String logoPath = "d7c42580-6538-4605-9ad8-116a61982644/hdc1/image002.mrc";
    private String authority = "10.5072";
    private String identifier = "F2ABCDEF";

    @BeforeEach
    public void setUp() {

        // Managed Globus Store

        // Nonsense endpoint/paths
        System.setProperty("dataverse.files.globusm." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH,
                "d7c42580-6538-4605-9ad8-116a61982644/hdc1");
        System.setProperty("dataverse.files.globusm.managed", "true");

        // Remote Store
        System.setProperty("dataverse.files.globusr.managed", "false");
        System.setProperty(
                "dataverse.files.globusr." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS,
                "d7c42580-6538-4605-9ad8-116a61982644/hdc1");

        dataset = MocksFactory.makeDataset();
        dataset.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, authority, identifier, "/",
                AbstractDOIProvider.DOI_RESOLVER_URL, null));
        mDatafile = MocksFactory.makeDataFile();
        mDatafile.setOwner(dataset);
        mDatafile.setStorageIdentifier("globusm://" + baseStoreId1);

        rDatafile = MocksFactory.makeDataFile();
        rDatafile.setOwner(dataset);
        rDatafile.setStorageIdentifier("globusr://" + baseStoreId2 + "//" + logoPath);
        List<DataFile> files = new ArrayList<DataFile>();
        files.add(mDatafile);
        files.add(rDatafile);
        dataset.setFiles(files);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("dataverse.files.globusm." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH);
        System.clearProperty("dataverse.files.globusm.managed");
        System.clearProperty("dataverse.files.globusr.managed");
        System.clearProperty(
                "dataverse.files.globusr." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS);
    }

    
    @Test
    public void testgetFilesMap() {
        
        JsonObject jo = GlobusUtil.getFilesMap(dataset.getFiles(), dataset);
        System.out.println(JsonUtil.prettyPrint(jo));
        assertEquals(jo.getString(Long.toString(mDatafile.getId())), "d7c42580-6538-4605-9ad8-116a61982644/hdc1/10.5072/F2ABCDEF/182ad2bda2f-c3508e719076");
        assertEquals(jo.getString(Long.toString(rDatafile.getId())), logoPath);
    }
}
