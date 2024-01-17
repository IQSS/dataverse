package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.engine.command.impl.LocalSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BagIT {

    static String bagitExportDir = "/tmp";

    @BeforeAll
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response setArchiverClassName = UtilIT.setSetting(SettingsServiceBean.Key.ArchiverClassName, LocalSubmitToArchiveCommand.class.getCanonicalName());
        setArchiverClassName.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setArchiverSettings = UtilIT.setSetting(SettingsServiceBean.Key.ArchiverSettings, ":BagItLocalPath, :BagGeneratorThreads");
        setArchiverSettings.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setBagItLocalPath = UtilIT.setSetting(":BagItLocalPath", bagitExportDir);
        setBagItLocalPath.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testBagItExport() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        Response archiveDataset = UtilIT.archiveDataset(datasetPid, "1.0", apiToken);
        archiveDataset.prettyPrint();
        archiveDataset.then().assertThat().statusCode(OK.getStatusCode());

        // spaceName comes from LocalSubmitToArchiveCommand
        String spaceName = datasetPid.replace(':', '-').replace('/', '-')
                .replace('.', '-').toLowerCase();
        // spacename: doi-10-5072-fk2-fosg5q

        String pathToZip = bagitExportDir + "/" + spaceName + "v1.0" + ".zip";

        try {
            // give the bag time to generate
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }

        // A bag could look like this:
        //doi-10-5072-FK2-DKUTDUv-1-0/data/
        //doi-10-5072-FK2-DKUTDUv-1-0/data/Darwin's Finches/
        //doi-10-5072-FK2-DKUTDUv-1-0/metadata/
        //doi-10-5072-FK2-DKUTDUv-1-0/metadata/pid-mapping.txt
        //doi-10-5072-FK2-DKUTDUv-1-0/manifest-md5.txt
        //doi-10-5072-FK2-DKUTDUv-1-0/bagit.txt
        //doi-10-5072-FK2-DKUTDUv-1-0/metadata/oai-ore.jsonld
        //doi-10-5072-FK2-DKUTDUv-1-0/metadata/datacite.xml
        //doi-10-5072-FK2-DKUTDUv-1-0/bag-info.txt
        // ---
        // bag-info.txt could look like this:
        //Contact-Name: Finch, Fiona
        //Contact-Email: finch@mailinator.com
        //Source-Organization: Dataverse Installation (<Site Url>)
        //Organization-Address: <Full address>
        //Organization-Email: <Email address>
        //External-Description: Darwin's finches (also known as the Galápagos finches) are a group of about
        // fifteen species of passerine birds.
        //Bagging-Date: 2023-11-14
        //External-Identifier: https://doi.org/10.5072/FK2/LZIGBC
        //Bag-Size: 0 bytes
        //Payload-Oxum: 0.0
        //Internal-Sender-Identifier: Root:Darwin's Finches
        Response downloadBag = UtilIT.downloadTmpFile(pathToZip, apiToken);
        downloadBag.then().assertThat().statusCode(OK.getStatusCode());
        Path outputPath = Paths.get("/tmp/foo.zip");
        java.nio.file.Files.copy(downloadBag.getBody().asInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);

        ZipFile zipFile = new ZipFile(outputPath.toString());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        String sourceOrg = null;
        String orgAddress = null;
        String orgEmail = null;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            System.out.println("name: " + name);
            if (name.endsWith("bag-info.txt")) {
                InputStream stream = zipFile.getInputStream(entry);
                Scanner s = new Scanner(stream).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                System.out.println("result: " + result);
                String[] lines = result.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Source-Organization")) {
                        sourceOrg = line;
                    } else if (line.startsWith("Organization-Address")) {
                        orgAddress = line;
                    } else if (line.startsWith("Organization-Email")) {
                        orgEmail = line;
                    } else {
                    }
                }
            }
        }
        assertEquals("Source-Organization: Dataverse Installation (<Site Url>)", sourceOrg.trim());
        assertEquals("Organization-Address: <Full address>", orgAddress.trim());
        assertEquals("Organization-Email: <Email address>", orgEmail.trim());
    }

    @AfterAll
    public static void tearDownClass() {

        // Not checking if delete happened. Hopefully, it did.
        UtilIT.deleteSetting(SettingsServiceBean.Key.ArchiverClassName);
        UtilIT.deleteSetting(SettingsServiceBean.Key.ArchiverSettings);
        UtilIT.deleteSetting(":BagItLocalPath");

    }

}