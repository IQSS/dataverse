/*
 * SPDX-License-Identifier: Apache 2.0
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.mocks.MocksFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.io.IOException;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class GlobusOverlayAccessIOTest {

    @Mock

    private Dataset dataset;
    private DataFile mDatafile;
    private DataFile rDatafile;
    private String baseStoreId1 = "182ad2bda2f-c3508e719076";
    private String baseStoreId2 = "182ad2bda2f-c3508e719077";
    private String logoPath = "d7c42580-6538-4605-9ad8-116a61982644/hdc1/image002.mrc";
    private String authority = "10.5072";
    private String identifier = "F2ABCDEF";

    @BeforeAll
    public static void setUp() {
        // Base Store
        System.setProperty("dataverse.files.base.type", DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
        System.setProperty("dataverse.files.base.label", "default");
        System.setProperty("dataverse.files.base.directory", "/tmp/files");

        // Managed Globus Store

        // Nonsense endpoint/paths
        System.setProperty("dataverse.files.globusm." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH,
                "d7c42580-6538-4605-9ad8-116a61982644/hdc1");
        // Nonsense value of the right form
        System.setProperty("dataverse.files.globusm.globus-token",
                "NzM2NTQxMDMtOTg1Yy00NDgzLWE1MTYtYTJlNDk0ZmI3MDhkOkpJZGZaZGxMZStQNUo3MTRIMDY2cDh6YzIrOXI2RmMrbFR6UG0zcSsycjA9");
        System.setProperty("dataverse.files.globusm.remote-store-name", "GlobusEndpoint1");
        System.setProperty("dataverse.files.globusm.type", "globus");
        System.setProperty("dataverse.files.globusm.managed", "true");
        System.setProperty("dataverse.files.globusm.base-store", "base");
        System.setProperty("dataverse.files.globusm.label", "globusManaged");

        // Remote Store
        System.setProperty("dataverse.files.globusr.type", "globus");
        System.setProperty("dataverse.files.globusr.base-store", "base");
        System.setProperty("dataverse.files.globusr.managed", "false");
        System.setProperty("dataverse.files.globusr.label", "globusRemote");
        System.setProperty(
                "dataverse.files.globusr." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS,
                "d7c42580-6538-4605-9ad8-116a61982644/hdc1");
        System.setProperty("dataverse.files.globusr.remote-store-name", "DemoDataCorp");

    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty("dataverse.files.base.type");
        System.clearProperty("dataverse.files.base.label");
        System.clearProperty("dataverse.files.base.directory");
        System.clearProperty("dataverse.files.globusm." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH);
        System.clearProperty("dataverse.files.globusm.globus-token");
        System.clearProperty("dataverse.files.globusm.remote-store-name");
        System.clearProperty("dataverse.files.globusm.type");
        System.clearProperty("dataverse.files.globusm.managed");
        System.clearProperty("dataverse.files.globusm.base-store");
        System.clearProperty("dataverse.files.globusm.label");
        System.clearProperty("dataverse.files.globusr.type");
        System.clearProperty("dataverse.files.globusr.base-store");
        System.clearProperty("dataverse.files.globusr.managed");
        System.clearProperty("dataverse.files.globusm.label");
        System.clearProperty(
                "dataverse.files.globusr." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS);
        System.clearProperty("dataverse.files.globusr.remote-store-name");
    }

    @Test
    void testGlobusOverlayIdentifiers() throws IOException {

        dataset = MocksFactory.makeDataset();
        dataset.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, authority, identifier, "/",
                DOIServiceBean.DOI_RESOLVER_URL, null));
        mDatafile = MocksFactory.makeDataFile();
        mDatafile.setOwner(dataset);
        mDatafile.setStorageIdentifier("globusm://" + baseStoreId1);

        rDatafile = MocksFactory.makeDataFile();
        rDatafile.setOwner(dataset);
        rDatafile.setStorageIdentifier("globusr://" + baseStoreId2 + "//" + logoPath);

        assertTrue(GlobusOverlayAccessIO.isValidIdentifier("globusm", mDatafile.getStorageIdentifier()));
        assertTrue(GlobusOverlayAccessIO.isValidIdentifier("globusr", rDatafile.getStorageIdentifier()));
        assertFalse(GlobusOverlayAccessIO.isValidIdentifier("globusm", "globusr://localid//../of/the/hill"));
        assertFalse(GlobusOverlayAccessIO.isValidIdentifier("globusr",
                rDatafile.getStorageIdentifier().replace("hdc1", "")));

        // We can read the storageIdentifier and get the driver
        assertTrue(mDatafile.getStorageIdentifier()
                .startsWith(DataAccess.getStorageDriverFromIdentifier(mDatafile.getStorageIdentifier())));
        assertTrue(rDatafile.getStorageIdentifier()
                .startsWith(DataAccess.getStorageDriverFromIdentifier(rDatafile.getStorageIdentifier())));

        // We can get the driver type from it's ID
        assertTrue(DataAccess.getDriverType("globusm").equals(System.getProperty("dataverse.files.globusm.type")));
        assertTrue(DataAccess.getDriverType("globusr").equals(System.getProperty("dataverse.files.globusr.type")));

        // When we get a StorageIO for the file, it is the right type
        StorageIO<DataFile> mStorageIO = DataAccess.getStorageIO(mDatafile);
        assertTrue(mStorageIO instanceof GlobusOverlayAccessIO);
        StorageIO<DataFile> rStorageIO = DataAccess.getStorageIO(rDatafile);
        assertTrue(rStorageIO instanceof GlobusOverlayAccessIO);

        // When we use it, we can get properties like the remote store name
        assertTrue(mStorageIO.getRemoteStoreName()
                .equals(System.getProperty("dataverse.files.globusm.remote-store-name")));
        assertTrue(rStorageIO.getRemoteStoreName()
                .equals(System.getProperty("dataverse.files.globusr.remote-store-name")));

        // Storage Locations are correct
        String mLocation = mStorageIO.getStorageLocation();
        assertEquals("globusm:///" + dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage()
                + "/" + baseStoreId1, mLocation);
        String rLocation = rStorageIO.getStorageLocation();
        assertEquals("globusr://" + baseStoreId2 + "//" + logoPath, rLocation);

        // If we ask for the path for an aux file, it is correct
        System.out.println(Paths.get(System.getProperty("dataverse.files.file.directory", "/tmp/files"), authority,
                identifier, baseStoreId1 + ".auxobject").toString());
        System.out.println(mStorageIO.getAuxObjectAsPath("auxobject").toString());
        assertTrue(Paths.get(System.getProperty("dataverse.files.base.directory", "/tmp/files"), authority, identifier,
                baseStoreId1 + ".auxobject").equals(mStorageIO.getAuxObjectAsPath("auxobject")));
        assertTrue(Paths.get(System.getProperty("dataverse.files.base.directory", "/tmp/files"), authority, identifier,
                baseStoreId2 + ".auxobject").equals(rStorageIO.getAuxObjectAsPath("auxobject")));
    }
}
