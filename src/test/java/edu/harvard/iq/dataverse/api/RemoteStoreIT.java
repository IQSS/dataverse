package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RemoteStoreIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testRemoteStore() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        /**
         * Note that you must configure various JVM options for this to work:
         *
         * <jvm-options>-Ddataverse.files.trsa.type=remote</jvm-options>
         * <jvm-options>-Ddataverse.files.trsa.label=trsa</jvm-options>
         * <jvm-options>-Ddataverse.files.trsa.base-url=https://qdr.syr.edu</jvm-options>
         * <jvm-options>-Ddataverse.files.trsa.base-store=file</jvm-options>
         *
         * In practice, most installation will also enable download-redirect
         * (below) to prevent the files from being streamed through Dataverse!
         *
         * <jvm-options>-Ddataverse.files.trsa.download-redirect=true</jvm-options>
         */
        JsonObjectBuilder remoteFileJson = Json.createObjectBuilder()
                .add("description", "A remote image.")
                .add("storageIdentifier", "trsa://themes/custom/qdr/images/CoreTrustSeal-logo-transparent.png")
                .add("checksumType", "MD5")
                .add("md5Hash", "509ef88afa907eaf2c17c1c8d8fde77e")
                .add("label", "testlogo.png")
                .add("fileName", "testlogo.png")
                .add("mimeType", "image/png");

        Response addRemoteFile = UtilIT.addRemoteFile(datasetId.toString(), remoteFileJson.build().toString(), apiToken);
        addRemoteFile.prettyPrint();
        addRemoteFile.then().assertThat()
                .statusCode(OK.getStatusCode());
        System.out.println("done!");
    }

}
