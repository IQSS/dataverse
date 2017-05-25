package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import java.io.UnsupportedEncodingException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DataCaptureModuleUtilTest {

    @Test
    public void testRsyncSupportEnabled() {
        System.out.println("rsyncSupportEnabled");
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled(null));
        assertEquals(true, DataCaptureModuleUtil.rsyncSupportEnabled("dcm/rsync+ssh"));
        // We haven't finalized what the separator will be yet.
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("NATIVE:dcm/rsync+ssh"));
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("NATIVE,dcm/rsync+ssh"));
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("NATIVE"));
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("junk"));
    }

    @Test
    public void testGenerateJsonForUploadRequest() {
        System.out.println("generateJsonForUploadRequest");
        AuthenticatedUser user = makeAuthenticatedUser("Ralph", "Rsync");
        Dataset dataset = new Dataset();
        dataset.setId(42l);
        JsonObject result = DataCaptureModuleUtil.generateJsonForUploadRequest(user, dataset);
        assertEquals(42, result.getInt("datasetIdentifier"));
        int userId = result.getInt("userId");
        assertTrue(Integer.MIN_VALUE <= userId && userId <= Integer.MAX_VALUE);
    }

    @Test
    public void testGetScriptFromRequestOk() throws UnsupportedEncodingException {
        System.out.println("getScriptFromRequestOk");
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        org.apache.http.HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null), null);
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("userId", 42);
        jab.add("datasetIdentifier", 123);
        jab.add("script", "#!/bin/sh");
        response.setEntity(new StringEntity(jab.build().toString()));
        HttpResponse<JsonNode> httpResponse = new HttpResponse<>(response, JsonNode.class);
        ScriptRequestResponse result = DataCaptureModuleUtil.getScriptFromRequest(httpResponse);
        assertEquals(200, result.getHttpStatusCode());
        assertEquals(123, result.getDatasetId());
        assertEquals(42, result.getUserId());
        assertEquals("#!/bin/sh", result.getScript());
    }

    @Test
    public void testGetScriptFromRequestNotFound() throws UnsupportedEncodingException {
        System.out.println("getScriptFromRequestNotFound");
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        org.apache.http.HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, null), null);
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("userId", 42);
        jab.add("datasetIdentifier", 123);
        jab.add("script", "#!/bin/sh");
        response.setEntity(new StringEntity(jab.build().toString()));
        HttpResponse<JsonNode> httpResponse = new HttpResponse<>(response, JsonNode.class);
        ScriptRequestResponse result = DataCaptureModuleUtil.getScriptFromRequest(httpResponse);
        assertEquals(404, result.getHttpStatusCode());
        assertEquals(-1, result.getDatasetId());
        assertEquals(-1, result.getUserId());
        assertEquals(null, result.getScript());
    }

    @Test
    public void testMakeUploadRequest() throws UnsupportedEncodingException {
        System.out.println("makeUploadRequest");
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        org.apache.http.HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null), null);
        response.setEntity(new StringEntity("received"));
        HttpResponse<String> httpResponse = new HttpResponse<>(response, String.class);
        UploadRequestResponse result = DataCaptureModuleUtil.makeUploadRequest(httpResponse);
        assertEquals(200, result.getHttpStatusCode());
        assertEquals("received", result.getResponse());
    }

}
