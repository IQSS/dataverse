package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
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
        // Comma sepratated lists of upload methods are supported.
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("native/http:dcm/rsync+ssh"));
        assertEquals(true, DataCaptureModuleUtil.rsyncSupportEnabled("native/http,dcm/rsync+ssh"));
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("native/http"));
        assertEquals(false, DataCaptureModuleUtil.rsyncSupportEnabled("junk"));
    }

    @Test
    public void testGenerateJsonForUploadRequest() {
        System.out.println("generateJsonForUploadRequest");
        AuthenticatedUser user = makeAuthenticatedUser("Ralph", "Rsync");
        Dataset dataset = new Dataset();
        dataset.setIdentifier("42");
        JsonObject result = DataCaptureModuleUtil.generateJsonForUploadRequest(user, dataset);
        assertEquals("42", result.getString("datasetIdentifier") );
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
        jab.add("datasetIdentifier", "123");
        jab.add("script", "#!/bin/sh");
        response.setEntity(new StringEntity(jab.build().toString()));
        HttpResponse<JsonNode> httpResponse = new HttpResponse<>(response, JsonNode.class);
        ScriptRequestResponse result = DataCaptureModuleUtil.getScriptFromRequest(httpResponse);
        assertEquals(200, result.getHttpStatusCode());
        assertEquals("123", result.getDatasetIdentifier() );
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
        jab.add("datasetIdentifier", "123");
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

    @Test
    public void testGetMessageFromException() {
        System.out.println("getMessageFromException");
        // preferred form
        assertEquals("message1 was caused by innerExceptionMessage", DataCaptureModuleUtil.getMessageFromException(new DataCaptureModuleException("message1", new NullPointerException("innerExceptionMessage"))));
        // suboptimal messages
        assertEquals("message1 was caused by null", DataCaptureModuleUtil.getMessageFromException(new DataCaptureModuleException("message1", new NullPointerException())));
        assertEquals("java.lang.NullPointerException", DataCaptureModuleUtil.getMessageFromException(new DataCaptureModuleException(null, new NullPointerException())));
        assertEquals("DataCaptureModuleException was null!", DataCaptureModuleUtil.getMessageFromException(null));
        assertEquals("edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleException", DataCaptureModuleUtil.getMessageFromException(new DataCaptureModuleException(null, null)));
        assertEquals("edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleException: message1", DataCaptureModuleUtil.getMessageFromException(new DataCaptureModuleException("message1", null)));
    }

    @Test
    public void testScriptName() {
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        dataset.setIdentifier("KYHURW");
        datasetVersion.setDataset(dataset);
        assertEquals("upload-KYHURW.bash", DataCaptureModuleUtil.getScriptName(datasetVersion));
    }

}
