package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import java.io.StringReader;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import javax.json.JsonObject;
import static java.lang.Thread.sleep;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 * These tests are not expected to pass unless you have a Data Capture Module
 * (DCM) installed and configured properly or a mock version of the DCM. They
 * are intended to help a developer get set up for DCM development.
 */
public class DataCaptureModuleServiceBeanIT {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getCanonicalName());

    @Test
    public void testUploadRequestAndScriptRequest() throws InterruptedException, DataCaptureModuleException {
        // The DCM Vagrant box runs on port 8888: https://github.com/sbgrid/data-capture-module/blob/master/Vagrantfile
        String dcmVagrantUrl = "http://localhost:8888";
        // The DCM mock runs on port 5000: https://github.com/sbgrid/data-capture-module/blob/master/doc/mock.md
        String dcmMockUrl = "http://localhost:5000";
        String dcmBaseUrl = dcmMockUrl;
        DataCaptureModuleServiceBean dataCaptureModuleServiceBean = new DataCaptureModuleServiceBean();

        // Step 1: Upload request
        AuthenticatedUser authenticatedUser = makeAuthenticatedUser("Lauren", "Ipsum");
        Dataset dataset = new Dataset();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long timeInMillis = calendar.getTimeInMillis();
        String ident = Long.toString(timeInMillis);
        dataset.setIdentifier(ident);
        String jsonString = DataCaptureModuleUtil.generateJsonForUploadRequest(authenticatedUser, dataset).toString();
        logger.info("jsonString: " + jsonString);
        UploadRequestResponse uploadRequestResponse = dataCaptureModuleServiceBean.requestRsyncScriptCreation(jsonString, dcmBaseUrl + DataCaptureModuleServiceBean.uploadRequestPath);
        System.out.println("out: " + uploadRequestResponse.getResponse());
        assertEquals(200, uploadRequestResponse.getHttpStatusCode());
        String uploadRequestResponseString = uploadRequestResponse.getResponse();
        JsonReader jsonReader = Json.createReader(new StringReader((String) uploadRequestResponseString));
        JsonObject jsonObject = jsonReader.readObject();
        assertEquals("OK", jsonObject.getString("status"));

        // If you comment this out, expect to see a 404 when you try to download the script.
        sleep(DataCaptureModuleServiceBean.millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls);

        // Step 2: Script request.
        ScriptRequestResponse scriptRequestResponseGood = dataCaptureModuleServiceBean.retreiveRequestedRsyncScript(dataset.getIdentifier(), dcmBaseUrl + DataCaptureModuleServiceBean.scriptRequestPath);
        System.out.println("script: " + scriptRequestResponseGood.getScript());
        assertNotNull(scriptRequestResponseGood.getScript());
        assertTrue(scriptRequestResponseGood.getScript().startsWith("#!"));

    }

    /**
     * In earlier iterations, we had the "dataCaptureModule/checksumValidation"
     * Datasets API calling into the "batch/jobs/import" API over port 8080.
     * This was refactored into a command in f8809c3 but this test might be
     * useful for testing the "batch/jobs/import" API directly.
     */
    @Test
    public void testStartFileSystemImportJob() throws InterruptedException, DataCaptureModuleException {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("OSQSB9");
        dataset.setId(728l);
        String url = "http://localhost:8080/api/batch/jobs/import/datasets/files/:persistentId?persistentId=" + dataset.getGlobalId().asString();
        System.out.print("url: " + url);
        String uploadFolder = "OSQSB9";
        String apiToken = "b440cc45-0ce9-4ae6-aabf-72f50fb8b8f2";
        int totalSize = 54321;
        JsonObject jsonObject = startFileSystemImportJob(dataset.getId(), url, uploadFolder, totalSize, apiToken);
        System.out.println("json: " + jsonObject);
    }

    private JsonObject startFileSystemImportJob(long datasetId, String url, String uploadFolder, int totalSize, String apiToken) throws DataCaptureModuleException {
        logger.info("Using URL " + url);
        try {
            HttpResponse<JsonNode> unirestRequest = Unirest.post(url)
                    .queryString("uploadFolder", uploadFolder)
                    .queryString("totalSize", totalSize)
                    .queryString("key", apiToken)
                    .asJson();
            return startFileSystemImportJob(unirestRequest);
        } catch (UnirestException ex) {
            String error = "Error calling " + url + ": " + ex;
            logger.info(error);
            throw new DataCaptureModuleException(error, ex);
        }
    }

    private static JsonObject startFileSystemImportJob(HttpResponse<JsonNode> uploadRequest) {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("status", uploadRequest.getStatus());
        int status = uploadRequest.getStatus();
        JsonNode body = uploadRequest.getBody();
        logger.info("Got " + status + " with body: " + body);
        if (status != 200) {
            jab.add("message", body.getObject().getString("message"));
            return jab.build();
        }
        jab.add("executionId", body.getObject().getJSONObject("data").getLong("executionId"));
        jab.add("message", body.getObject().getJSONObject("data").getString("message"));
        return jab.build();
    }

}
