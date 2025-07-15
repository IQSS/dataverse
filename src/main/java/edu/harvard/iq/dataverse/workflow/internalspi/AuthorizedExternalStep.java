package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import edu.harvard.iq.dataverse.workflows.WorkflowUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.net.URIBuilder;

/**
 * A workflow step that sends a HTTP request, and then pauses, waiting for an
 * application/json response. This step is allowed to act as the user causing
 * the workflow launch and can make changes to the dataset it is associated
 * with. To do so, it must send the invocationId in a manner analogous to
 * sending an api key. See guides.
 * 
 */
public class AuthorizedExternalStep implements WorkflowStep {
    private static final Logger logger = Logger.getLogger(AuthorizedExternalStep.class.getName());
    private final Map<String,String> params;

    public AuthorizedExternalStep(Map<String, String> paramSet) {
        params = new HashMap<>(paramSet);
    }
    
    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // build method
            ClassicHttpRequest request = buildMethod(false, context);
            // execute
            return client.execute(request, response -> {
                int responseStatus = response.getCode();
                if (responseStatus >= 200 && responseStatus < 300) {
                    // HTTP OK range
                    Map<String, String> data = new HashMap<>();
                    //Allow external client to use invocationId as a key to act on the user's behalf
                    data.put(PendingWorkflowInvocation.AUTHORIZED, "true");
                    return new Pending(data);
                } else {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
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
        return WorkflowUtil.parseResponse(externalData);
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // build method
            ClassicHttpRequest request = buildMethod(true, context);
            
            // execute
            client.execute(request, response -> {
                int responseStatus = response.getCode();
                if (responseStatus < 200 || responseStatus >= 300) {
                    // out of HTTP OK range
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    Logger.getLogger(AuthorizedExternalStep.class.getName()).log(Level.WARNING, 
                            "Bad response from remote server while rolling back step: {0}", responseBody);
                }
                return null;
            });
        } catch (Exception ex) {
            Logger.getLogger(AuthorizedExternalStep.class.getName()).log(Level.WARNING, "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }
    
    ClassicHttpRequest buildMethod(boolean rollback, WorkflowContext ctxt) throws Exception {
        String methodName = params.getOrDefault("method" + (rollback ? "-rollback":""), "GET").trim().toUpperCase();
        
        Map<String,String> templateParams = new HashMap<>();
        templateParams.put("invocationId", ctxt.getInvocationId());
        templateParams.put("dataset.id", Long.toString(ctxt.getDataset().getId()));
        templateParams.put("dataset.identifier", ctxt.getDataset().getIdentifier());
        templateParams.put("dataset.globalId", ctxt.getDataset().getGlobalId().toString());
        templateParams.put("dataset.displayName", ctxt.getDataset().getDisplayName());
        templateParams.put("dataset.citation", ctxt.getDataset().getCitation());
        templateParams.put("minorVersion", Long.toString(ctxt.getNextMinorVersionNumber()));
        templateParams.put("majorVersion", Long.toString(ctxt.getNextVersionNumber()));
        templateParams.put("releaseStatus", (ctxt.getType()==TriggerType.PostPublishDataset) ? "done":"in-progress");
        templateParams.put("language", BundleUtil.getDefaultLocale().getISO3Language());
    
        String urlKey = rollback ? "rollbackUrl" : "url";
        String urlTemplate = params.get(urlKey);
        String processedUrl = process(urlTemplate, templateParams);
        
        URI uri;
        try {
            uri = new URIBuilder(processedUrl).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Illegal URL: '" + processedUrl + "'", ex);
        }
    
        ClassicHttpRequest request;
        switch (methodName) {
            case "GET":    request = new HttpGet(uri); break;
            case "POST":   request = new HttpPost(uri); break;
            case "PUT":    request = new HttpPut(uri); break;
            case "DELETE": request = new HttpDelete(uri); break;
            default: throw new IllegalStateException("Unsupported HTTP method: '" + methodName + "'");
        }
    
        request.setHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));
    
        return request;
    }
    
    String process(String template, Map<String,String> values ) {
        String curValue = template;
        for ( Map.Entry<String, String> ent : values.entrySet() ) {
            String val = ent.getValue();
            if ( val == null ) { 
                val = "" ;
            }
            String varRef = "${" + ent.getKey() + "}";
            while ( curValue.contains(varRef) ) {
                curValue = curValue.replace(varRef, val);
            }
        }
        
        return curValue;
    }
}