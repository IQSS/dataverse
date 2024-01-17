package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import static io.restassured.path.json.JsonPath.with;
import io.restassured.response.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FitsIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testAstroFieldsFromFits() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("astrophysics"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        // "FOS 2 x 2064 primary array spectrum containing the flux and wavelength arrays, plus a small table extension"
        // from https://fits.gsfc.nasa.gov/fits_samples.html
        String pathToFile = "src/test/resources/fits/FOSy19g0309t_c2f.fits";

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat().statusCode(OK.getStatusCode());

        Response getJson = UtilIT.nativeGet(datasetId, apiToken);
        getJson.prettyPrint();
        getJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].description", equalTo("FITS file, 2 HDUs total:\nThe primary HDU; 1 Table HDU(s) 1 Image HDU(s); \nThe following recognized metadata keys have been found in the FITS file:\nCRVAL2; NAXIS; INSTRUME; NAXIS1; NAXIS0; EXPTIME; CD1_1; CRVAL1; TARGNAME; DATE-OBS; \n"))
                .body("data.latestVersion.metadataBlocks.astrophysics.fields[0].value[0]", equalTo("Image"));

        // a bit more precise than the check for "Image" above (but annoyingly fiddly)
        List<JsonObject> astroTypeFromNativeGet = with(getJson.body().asString()).param("astroType", "astroType")
                .getJsonObject("data.latestVersion.metadataBlocks.astrophysics.fields.findAll { fields -> fields.typeName == astroType }");
        Map firstAstroTypeFromNativeGet = astroTypeFromNativeGet.get(0);
        assertTrue(firstAstroTypeFromNativeGet.toString().contains("Image"));

        List<JsonObject> coverageTemportalFromNativeGet = with(getJson.body().asString()).param("coverageTemporal", "coverage.Temporal")
                .getJsonObject("data.latestVersion.metadataBlocks.astrophysics.fields.findAll { fields -> fields.typeName == coverageTemporal }");
        Map firstcoverageTemporalFromNativeGet = coverageTemportalFromNativeGet.get(0);
        assertTrue(firstcoverageTemporalFromNativeGet.toString().contains("1993"));

    }

}
