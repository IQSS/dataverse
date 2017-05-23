package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import junit.framework.Assert;
import org.junit.Test;

public class DataCaptureModuleServiceBeanIT {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getCanonicalName());

    @Test
    public void testUrDotPy() {

        JsonObjectBuilder jab = Json.createObjectBuilder();
        // The general rule should be to always pass the user id and dataset id to the DCM.
        jab.add("userId", 42);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long timeInMillis = calendar.getTimeInMillis();
        jab.add("datasetId", timeInMillis);
        jab.add("datasetIdentifier", timeInMillis);
        JsonObject jsonObject = jab.build();
        String jsonString = jsonObject.toString();
        JsonNode jsonNode = new JsonNode(jsonString);
        try {
            // curl -H 'Content-Type: application/json' -X POST -d '{"datasetId":"42", "userId":"1642","datasetIdentifier":"42"}' http://localhost/ur.py
            HttpResponse<String> uploadRequest = Unirest.post("http://localhost:8888" + "/ur.py")
                    //                    .body(jsonString)
                    .body(jsonNode)
                    //                    .asJson();
                    .asString();
            System.out.println("out: " + uploadRequest.getBody());
        } catch (UnirestException ex) {
            Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Test
    public void testSrDotPy() {
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
//        JsonNode jsonNode = new JsonNode(jsonString);
        try {
            HttpResponse<String> uploadRequest = Unirest.post("http://localhost:8888" + "/sr.py")
                    //                    .body(jsonString)
                    //                    .body(bodyString)
                    .field("datasetIdentifier", datasetId)
                    //                    .asJson();
                    .asString();
            System.out.println("status: " + uploadRequest.getStatus());
            System.out.println("out:\n" + uploadRequest.getBody());
        } catch (UnirestException ex) {
            Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Test
    public void testScriptRequestWorking() {
        long expectedToWork = 3813;
        ScriptRequestResponse scriptRequestResponseGood = DataCaptureModuleServiceBean.getRsyncScriptForDataset("http://localhost:8888", expectedToWork);
        System.out.println("script: " + scriptRequestResponseGood.getScript());
        Assert.assertTrue(scriptRequestResponseGood.getScript().startsWith("#!"));
    }

    @Test
    public void testScriptRequestNotWorking() {
        long notExpectedToWork = Long.MAX_VALUE;
        ScriptRequestResponse scriptRequestResponseBad = DataCaptureModuleServiceBean.getRsyncScriptForDataset("http://localhost:8888", notExpectedToWork);
        Assert.assertNull(scriptRequestResponseBad.getScript());
    }

}
