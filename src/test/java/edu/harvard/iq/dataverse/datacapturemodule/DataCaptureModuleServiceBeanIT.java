package edu.harvard.iq.dataverse.datacapturemodule;

import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.response.Response;
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
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
//        JsonNode jsonNode = new JsonNode(jsonString);
        try {
            HttpResponse<String> uploadRequest = Unirest.post("http://localhost:8888" + "/sr.py")
                    //                    .body(jsonString)
                    .body(bodyString)
                    //                    .asJson();
                    .asString();
            System.out.println("status: " + uploadRequest.getStatus());
            System.out.println("out:\n" + uploadRequest.getBody());
        } catch (UnirestException ex) {
            Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Test
    public void testSettingUnirest() {
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
//        JsonNode jsonNode = new JsonNode(jsonString);
        try {
            HttpResponse<String> uploadRequest = Unirest.put("http://localhost:8080/api/admin/settings")
                    //                    .body(jsonString)
                    .body(bodyString)
                    //                    .asJson();
                    .asString();
            System.out.println("status: " + uploadRequest.getStatus());
            System.out.println("out:\n" + uploadRequest.getBody());
        } catch (UnirestException ex) {
            Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testSettingRestAssured() {
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
        Response response = given().body(bodyString).when().post("http://localhost:8888/sr.py");
        response.prettyPrint();
        System.out.println("status: " + response.getStatusCode());
    }

}
