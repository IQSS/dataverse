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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

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
        HttpClient client = new HttpClient();
        
        try {
            // build method
            HttpMethodBase mtd = buildMethod(false, context);
            // execute
            int responseStatus = client.executeMethod(mtd);
            if (responseStatus>=200 && responseStatus<300 ) {
                // HTTP OK range
                Map<String, String> data = new HashMap<String, String> ();
                //Allow external client to use invocationId as a key to act on the user's behalf
                data.put(PendingWorkflowInvocation.AUTHORIZED, "true");
                return new Pending(data);
            } else {
                String responseBody = mtd.getResponseBodyAsString();
                return new Failure("Error communicating with server. Server response: " + responseBody + " (" + responseStatus + ").");
            }
            
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
        HttpClient client = new HttpClient();
        
        try {
            // build method
            HttpMethodBase mtd = buildMethod(true, context);
            
            // execute
            int responseStatus = client.executeMethod(mtd);
            if (responseStatus<200 || responseStatus>=300 ) {
                // out of HTTP OK range
                String responseBody = mtd.getResponseBodyAsString();
                Logger.getLogger(AuthorizedExternalStep.class.getName()).log(Level.WARNING, 
                        "Bad response from remote server while rolling back step: {0}", responseBody);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(AuthorizedExternalStep.class.getName()).log(Level.WARNING, "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }
    
    HttpMethodBase buildMethod(boolean rollback, WorkflowContext ctxt) throws Exception {
        String methodName = params.getOrDefault("method" + (rollback ? "-rollback":""), "GET").trim().toUpperCase();
        HttpMethodBase m = null;
        switch (methodName) {
            case "GET":    m = new GetMethod(); m.setFollowRedirects(true); break;
            case "POST":   m = new PostMethod(); break;
            case "PUT":    m = new PutMethod(); break;
            case "DELETE": m = new DeleteMethod(); m.setFollowRedirects(true); break;
            default: throw new IllegalStateException("Unsupported HTTP method: '" + methodName + "'");
        }
        
        
        Map<String,String> templateParams = new HashMap<>();
        templateParams.put( "invocationId", ctxt.getInvocationId() );
        templateParams.put( "dataset.id", Long.toString(ctxt.getDataset().getId()) );
        templateParams.put( "dataset.identifier", ctxt.getDataset().getIdentifier() );
        templateParams.put( "dataset.globalId", ctxt.getDataset().getGlobalId().toString() );
        templateParams.put( "dataset.displayName", ctxt.getDataset().getDisplayName() );
        templateParams.put( "dataset.citation", ctxt.getDataset().getCitation() );
        templateParams.put( "minorVersion", Long.toString(ctxt.getNextMinorVersionNumber()) );
        templateParams.put( "majorVersion", Long.toString(ctxt.getNextVersionNumber()) );
        templateParams.put( "releaseStatus", (ctxt.getType()==TriggerType.PostPublishDataset) ? "done":"in-progress" );
        templateParams.put( "language", BundleUtil.getDefaultLocale().getISO3Language());

        
        m.addRequestHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));
        
        String urlKey = rollback ? "rollbackUrl":"url";
        String url = params.get(urlKey);
        try {
            m.setURI(new URI(process(url,templateParams), true) );
        } catch (URIException ex) {
            throw new IllegalStateException("Illegal URL: '" + url + "'");
        }
        
        String bodyKey = (rollback ? "rollbackBody" : "body");
        if ( params.containsKey(bodyKey) && m instanceof EntityEnclosingMethod ) {
            String body = params.get(bodyKey);
            ((EntityEnclosingMethod)m).setRequestEntity(new StringRequestEntity(process( body, templateParams), params.getOrDefault("contentType", "text/plain"), StandardCharsets.UTF_8.name()));
        }
        
        return m;
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
