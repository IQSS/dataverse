package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import static edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult.OK;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class HttpSendReceiveClientStep implements WorkflowStep {
    private static final Logger logger = Logger.getLogger(HttpSendReceiveClientStep.class.getName());
    private final Map<String,String> params;

    public HttpSendReceiveClientStep(Map<String, String> paramSet) {
        params = new HashMap<>(paramSet);
    }
    
    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest request = buildRequest(false, context);
            // execute
            return client.execute(request, response -> {
            
                int responseStatus = response.getCode();
                if (responseStatus >= 200 && responseStatus < 300) {
                    return new Pending();
                } else {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return new Failure("Error communicating with server. Server response: " + responseBody + " (" + responseStatus + ").");
                }
            });
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error communicating with remote server: " + ex.getMessage(), ex);
            return new Failure("Error executing request: " + ex.getLocalizedMessage(), "Cannot communicate with remote server.");
        }
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        Pattern pat = Pattern.compile(params.get("expectedResponse"));
        String response = externalData.trim();
        if (pat.matcher(response).matches()) {
            return OK;
        } else {
            logger.log(Level.WARNING, "Remote system returned a bad response: {0}", externalData);
            return new Failure("Response from remote server did not match expected one (response:" + response + ")");
        }
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest request = buildRequest(true, context);
            
            client.execute(request, response -> {
                int responseStatus = response.getCode();
                if (responseStatus < 200 || responseStatus >= 300) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    logger.log(Level.WARNING, "Bad response from remote server while rolling back step: {0}", responseBody);
                }
                return null;
            });
        } catch (Exception ex) {
            logger.log(Level.WARNING, "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }
    
    ClassicHttpRequest buildRequest(boolean rollback, WorkflowContext ctxt) throws Exception {
        String methodName = params.getOrDefault("method" + (rollback ? "-rollback":""), "GET").trim().toUpperCase();
        
        Map<String,String> templateParams = new HashMap<>();
        templateParams.put("invocationId", ctxt.getInvocationId());
        templateParams.put("dataset.id", Long.toString(ctxt.getDataset().getId()));
        templateParams.put("dataset.identifier", ctxt.getDataset().getIdentifier());
        templateParams.put("dataset.globalId", ctxt.getDataset().getGlobalId().asString());
        templateParams.put("dataset.displayName", ctxt.getDataset().getDisplayName());
        templateParams.put("dataset.citation", ctxt.getDataset().getCitation());
        templateParams.put("minorVersion", Long.toString(ctxt.getNextMinorVersionNumber()));
        templateParams.put("majorVersion", Long.toString(ctxt.getNextVersionNumber()));
        templateParams.put("releaseStatus", (ctxt.getType()==TriggerType.PostPublishDataset) ? "done":"in-progress");
        
        String urlKey = rollback ? "rollbackUrl":"url";
        String url = params.get(urlKey);
        String processedUrl = process(url, templateParams);
        
        ClassicHttpRequest request;
        try {
            URI uri = new URI(processedUrl);
            switch (methodName) {
                case "GET":    request = new HttpGet(uri); break;
                case "POST":   request = new HttpPost(uri); break;
                case "PUT":    request = new HttpPut(uri); break;
                case "DELETE": request = new HttpDelete(uri); break;
                default: throw new IllegalStateException("Unsupported HTTP method: '" + methodName + "'");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Illegal URL: '" + processedUrl + "'", ex);
        }
        
        request.setHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));
        
        String bodyKey = (rollback ? "rollbackBody" : "body");
        if (params.containsKey(bodyKey) && (request instanceof HttpPost || request instanceof HttpPut)) {
            String body = params.get(bodyKey);
            request.setEntity(
                new StringEntity(process(body, templateParams), 
                    ContentType.create(request.getFirstHeader("Content-Type").getValue())
                )
            );
        }
        
        return request;
    }
    
    String process(String template, Map<String,String> values) {
        String curValue = template;
        for (Map.Entry<String, String> ent : values.entrySet()) {
            String val = ent.getValue();
            if (val == null) { 
                val = "";
            }
            String varRef = "${" + ent.getKey() + "}";
            while (curValue.contains(varRef)) {
                curValue = curValue.replace(varRef, val);
            }
        }
        
        return curValue;
    }
}