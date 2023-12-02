/*
 * Copyright 2018 Forschungszentrum Jülich GmbH
 * SPDX-License-Identifier: Apache 2.0
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private DataFile datafile;
    private DataFile localDatafile;
    private String baseStoreId = "182ad2bda2f-c3508e719076";
    private String logoPath = "image002.mrc";
    private String authority = "10.5072";
    private String identifier = "F2ABCDEF";

    @BeforeEach
    public void setUp() {
        System.setProperty("dataverse.files.globus." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH,
                "d8c42580-6528-4605-9ad8-116a61982644/hdc1");
        System.setProperty("dataverse.files.globus." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS,
                "d8c42580-6528-4605-9ad8-116a61982644/hdc1");

        System.setProperty("dataverse.files.globus.globus-token",
                "YTVlNzFjNzItYWVkYi00Mzg4LTkzNWQtY2NhM2IyODI2MzdmOnErQXRBeWNEMVM3amFWVnB0RlFnRk5zMTc3OFdDa3lGeVZPT3k0RDFpaXM9");
        System.setProperty("dataverse.files.globus.remote-store-name", "GlobusEndpoint1");
        System.setProperty("dataverse.files.globus.type", "globus");

        System.setProperty("dataverse.files.globus.managed", "true");

        System.setProperty("dataverse.files.globus.base-store", "file");
        System.setProperty("dataverse.files.file.type", DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
        System.setProperty("dataverse.files.file.directory", "/tmp/files");

        // System.setProperty("dataverse.files.test.type", "remote");
        System.setProperty("dataverse.files.globus.label", "globusTest");
        System.setProperty("dataverse.files.test.base-url", "https://demo.dataverse.org/resources");
        System.setProperty("dataverse.files.test.base-store", "file");
        System.setProperty("dataverse.files.test.download-redirect", "true");
        System.setProperty("dataverse.files.test.remote-store-name", "DemoDataCorp");
        System.setProperty("dataverse.files.globus.secret-key", "12345"); // Real keys should be much longer, more
                                                                          // random
        System.setProperty("dataverse.files.file.type", "file");
        System.setProperty("dataverse.files.file.label", "default");
        datafile = MocksFactory.makeDataFile();
        dataset = MocksFactory.makeDataset();
        dataset.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, authority, identifier, "/",
                DOIServiceBean.DOI_RESOLVER_URL, null));
        datafile.setOwner(dataset);
        datafile.setStorageIdentifier("globus://" + baseStoreId + "//" + logoPath);

        localDatafile = MocksFactory.makeDataFile();
        localDatafile.setOwner(dataset);
        localDatafile.setStorageIdentifier("globus://" + baseStoreId);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("dataverse.files.test.type");
        System.clearProperty("dataverse.files.test.label");
        System.clearProperty("dataverse.files.test.base-url");
        System.clearProperty("dataverse.files.test.base-store");
        System.clearProperty("dataverse.files.test.download-redirect");
        System.clearProperty("dataverse.files.test.label");
        System.clearProperty("dataverse.files.test.remote-store-name");
        System.clearProperty("dataverse.files.test.secret-key");
        System.clearProperty("dataverse.files.file.type");
        System.clearProperty("dataverse.files.file.label");
    }

    @Test
    void testGlobusOverlayFiles() throws IOException {
        System.clearProperty("dataverse.files.globus.managed");
        datafile.setStorageIdentifier(
                "globus://" + baseStoreId + "//d8c42580-6528-4605-9ad8-116a61982644/hdc1/" + logoPath);
        GlobusOverlayAccessIO<DvObject> gsio = new GlobusOverlayAccessIO<DvObject>(datafile, null, "globus");
        System.out.println("Size2 is " + gsio.retrieveSizeFromMedia());

        System.out.println(
                "NotValid: " + GlobusOverlayAccessIO.isValidIdentifier("globus", "globus://localid//../of/the/hill"));
        System.out.println(
                "ValidRemote: " + GlobusOverlayAccessIO.isValidIdentifier("globus", "globus://localid//of/the/hill"));
        System.setProperty("dataverse.files.globus.managed", "true");
        datafile.setStorageIdentifier("globus://" + baseStoreId + "//" + logoPath);
        System.out.println("ValidLocal: "
                + GlobusOverlayAccessIO.isValidIdentifier("globus", "globus://176e28068b0-1c3f80357c42"));

        // We can read the storageIdentifier and get the driver
        assertTrue(datafile.getStorageIdentifier()
                .startsWith(DataAccess.getStorageDriverFromIdentifier(datafile.getStorageIdentifier())));
        // We can get the driver type from it's ID
        assertTrue(DataAccess.getDriverType("globus").equals(System.getProperty("dataverse.files.globus.type")));
        // When we get a StorageIO for the file, it is the right type
        StorageIO<DataFile> storageIO = DataAccess.getStorageIO(localDatafile);
        assertTrue(storageIO instanceof GlobusOverlayAccessIO);
        // When we use it, we can get properties like the remote store name
        GlobusOverlayAccessIO<DataFile> globusIO = (GlobusOverlayAccessIO<DataFile>) storageIO;
        assertTrue(
                globusIO.getRemoteStoreName().equals(System.getProperty("dataverse.files.globus.remote-store-name")));

        String location = globusIO.getStorageLocation();
        assertEquals("globus:///" + dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage() + "/" + baseStoreId, location);
/*
        // TBD:
        // And can get a temporary download URL for the main file
        String signedURL = globusIO.generateTemporaryDownloadUrl(null, null, null);
        System.out.println(signedURL);
        // And the URL starts with the right stuff
        assertTrue(signedURL.startsWith(System.getProperty("dataverse.files.globus." + GlobusAccessibleStore.TRANSFER_ENDPOINT_WITH_BASEPATH) + "/" + logoPath));
        // And the signature is valid
        // assertTrue(
        // UrlSignerUtil.isValidUrl(signedURL, null, null,
        // System.getProperty("dataverse.files.globus.secret-key")));
        // And we get an unsigned URL with the right stuff with no key
        System.clearProperty("dataverse.files.globus.secret-key");
        String unsignedURL = globusIO.generateTemporaryDownloadUrl(null, null, null);
        assertTrue(unsignedURL.equals(System.getProperty("dataverse.files.globus.base-url") + "/" + logoPath));
*/
        // Once we've opened, we can get the file size (only works if the call to Globus
        // works)
        globusIO.open(DataAccessOption.READ_ACCESS);
        assertTrue(globusIO.getSize() > 0);
        // If we ask for the path for an aux file, it is correct
        System.out.println(Paths.get(System.getProperty("dataverse.files.file.directory", "/tmp/files"), authority,
                identifier, baseStoreId + ".auxobject").toString());
        System.out.println(globusIO.getAuxObjectAsPath("auxobject").toString());
        assertTrue(Paths.get(System.getProperty("dataverse.files.file.directory", "/tmp/files"), authority, identifier,
                baseStoreId + ".auxobject").equals(globusIO.getAuxObjectAsPath("auxobject")));
        IOException thrown = assertThrows(IOException.class, () -> DataAccess.getStorageIO(localDatafile),
                "Expected getStorageIO() to throw, but it didn't");
        // 'test' is the driverId in the IOException messages
        assertTrue(thrown.getMessage().contains("globus"));

    }

    @Test
    void testRemoteOverlayIdentifierFormats() throws IOException {
        System.clearProperty("dataverse.files.globus.managed");
        datafile.setStorageIdentifier(
                "globus://" + baseStoreId + "//d8c42580-6528-4605-9ad8-116a61982644/hdc1/" + logoPath);
        assertTrue(DataAccess.isValidDirectStorageIdentifier(datafile.getStorageIdentifier()));
        assertFalse(
                DataAccess.isValidDirectStorageIdentifier(datafile.getStorageIdentifier().replace("globus", "bad")));
        assertFalse(DataAccess.isValidDirectStorageIdentifier(localDatafile.getStorageIdentifier()));
        System.setProperty("dataverse.files.globus.managed", "true");
        assertTrue(DataAccess.isValidDirectStorageIdentifier(localDatafile.getStorageIdentifier()));

    }

}
