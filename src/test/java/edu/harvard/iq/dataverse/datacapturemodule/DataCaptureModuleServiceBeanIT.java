package edu.harvard.iq.dataverse.datacapturemodule;

import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.response.Response;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.Test;

public class DataCaptureModuleServiceBeanIT {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getCanonicalName());

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
    public void testRestAssured() {
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
        Response response = given().body(bodyString).when().post("http://localhost:8888/sr.py");
        response.prettyPrint();
        System.out.println("status: " + response.getStatusCode());
    }

    @Test
    public void testHttpURL2() throws MalformedURLException, ProtocolException, IOException {
        System.out.println("BEGIN");
        String datasetId = "3813";
        String bodyString = "datasetIdentifier=" + datasetId;
        byte[] postData = bodyString.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String request = "http://localhost:8888/sr.py";
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        InputStreamReader inputStreamReader = new InputStreamReader((InputStream) conn.getContent());
        String result = new BufferedReader(inputStreamReader)
                .lines().collect(Collectors.joining("\n"));
        System.out.println("result: " + result);
        System.out.println("END");
    }

}
