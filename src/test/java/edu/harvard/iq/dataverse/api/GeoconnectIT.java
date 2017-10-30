package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.CREATED;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class GeoconnectIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Ignore
    @Test
    public void checkMapLayerMetadatas() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response checkMapLayerMetadatas = UtilIT.checkMapLayerMetadatas(apiToken);
        checkMapLayerMetadatas.prettyPrint();
        checkMapLayerMetadatas.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Ignore
    @Test
    public void checkSingleMapLayerMetadata() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response checkMapLayerMetadatas = UtilIT.checkMapLayerMetadatas(1l, apiToken);
        checkMapLayerMetadatas.prettyPrint();
        checkMapLayerMetadatas.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Ignore
    @Test
    public void deleteMapFromFile() {
        String apiToken = "b5a144d8-b3a3-452b-9543-131360884281";
        long fileId = 2661;
        Response deleteMapFromFile = UtilIT.deleteMapFromFile(fileId, apiToken);
        deleteMapFromFile.prettyPrint();
        deleteMapFromFile.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void destroyDatasetWithMappedShapeFile() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response addMapLayerMetadata = UtilIT.addMapLayerMetadata(datasetId, apiToken);
        addMapLayerMetadata.prettyPrint();
        addMapLayerMetadata.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response makeSuperuser = UtilIT.makeSuperUser(username);
        makeSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response destroyDataset = UtilIT.destroyDataset(datasetId, apiToken);
        destroyDataset.prettyPrint();
        destroyDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

}
