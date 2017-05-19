package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

public class DataCaptureModuleServiceBeanIT {

    @Test
    public void testUrDotPy() {
        String jsonString = "[]";
        JsonNode jsonNode = new JsonNode(jsonString);
        try {
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
        String datasetId = "3856";
        String bodyString = "datasetIdentifier=" + datasetId;
//        JsonNode jsonNode = new JsonNode(jsonString);
        try {
            HttpResponse<String> uploadRequest = Unirest.post("http://localhost:8888" + "/sr.py")
                    //                    .body(jsonString)
                    .body(bodyString)
                    //                    .asJson();
                    .asString();
            System.out.println("out: " + uploadRequest.getBody());
        } catch (UnirestException ex) {
            Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
