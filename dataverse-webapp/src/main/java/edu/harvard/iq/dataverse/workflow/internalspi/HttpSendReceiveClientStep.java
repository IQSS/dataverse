package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;

/**
 * A workflow step that sends a HTTP request, and then pauses, waiting for a response.
 *
 * @author michael
 */
public class HttpSendReceiveClientStep implements WorkflowStep {

    private static final Logger logger = Logger.getLogger(HttpSendReceiveClientStep.class.getName());

    private final WorkflowStepParams params;
    private final DatasetVersionServiceBean versionsService;

    public HttpSendReceiveClientStep(WorkflowStepParams params, DatasetVersionServiceBean versionsService) {
        this.params = params;
        this.versionsService = versionsService;
    }

    @Override
    public WorkflowStepResult run(WorkflowExecutionContext context) {
        HttpClient client = new HttpClient();

        try {
            // build method
            HttpMethodBase mtd = buildMethod(false, context);
            // execute
            int responseStatus = client.executeMethod(mtd);
            if (responseStatus >= 200 && responseStatus < 300) {
                // HTTP OK range
                return new Pending();
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
    public WorkflowStepResult resume(WorkflowExecutionContext context, Map<String, String> internalData, String externalData) {
        Pattern pat = Pattern.compile(params.get("expectedResponse"));
        String response = externalData.trim();
        if (pat.matcher(response).matches()) {
            return new Success();
        } else {
            logger.log(Level.WARNING, "Remote system returned a bad reposonse: {0}", externalData);
            return new Failure("Response from remote server did not match expected one (response:" + response + ")");
        }
    }

    @Override
    public void rollback(WorkflowExecutionContext context, Failure reason) {
        HttpClient client = new HttpClient();

        try {
            // build method
            HttpMethodBase mtd = buildMethod(true, context);

            // execute
            int responseStatus = client.executeMethod(mtd);
            if (responseStatus < 200 || responseStatus >= 300) {
                // out of HTTP OK range
                String responseBody = mtd.getResponseBodyAsString();
                Logger.getLogger(HttpSendReceiveClientStep.class.getName()).log(Level.WARNING,
                                                                                "Bad response from remote server while rolling back step: {0}", responseBody);
            }

        } catch (Exception ex) {
            Logger.getLogger(HttpSendReceiveClientStep.class.getName()).log(Level.WARNING, "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }

    HttpMethodBase buildMethod(boolean rollback, WorkflowExecutionContext ctxt) throws Exception {
        String methodName = params.getOrDefault("method" + (rollback ? "-rollback" : ""), "GET").trim().toUpperCase();
        HttpMethodBase m = null;
        switch (methodName) {
            case "GET":
                m = new GetMethod();
                m.setFollowRedirects(true);
                break;
            case "POST":
                m = new PostMethod();
                break;
            case "PUT":
                m = new PutMethod();
                break;
            case "DELETE":
                m = new DeleteMethod();
                m.setFollowRedirects(true);
                break;
            default:
                throw new IllegalStateException("Unsupported HTTP method: '" + methodName + "'");
        }


        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("invocationId", ctxt.getInvocationId());
        templateParams.putAll(versionsService.withDatasetVersion(ctxt,
                datasetVersion -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("dataset.id", Long.toString(datasetVersion.getDataset().getId()));
                    params.put("dataset.identifier", datasetVersion.getDataset().getIdentifier());
                    params.put("dataset.globalId", datasetVersion.getDataset().getGlobalIdString());
                    params.put("dataset.displayName", datasetVersion.getDataset().getDisplayName());
                    params.put("dataset.citation", datasetVersion.getDataset().getCitation());
                    return params;
                }
        ).orElseGet(Collections::emptyMap));
        templateParams.put("minorVersion", Long.toString(ctxt.getMinorVersionNumber()));
        templateParams.put("majorVersion", Long.toString(ctxt.getVersionNumber()));
        templateParams.put("releaseStatus", (ctxt.getType() == PostPublishDataset) ? "done" : "in-progress");

        m.addRequestHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));

        String urlKey = rollback ? "rollbackUrl" : "url";
        String url = params.get(urlKey);
        try {
            m.setURI(new URI(process(url, templateParams), true));
        } catch (URIException ex) {
            throw new IllegalStateException("Illegal URL: '" + url + "'");
        }

        String bodyKey = (rollback ? "rollbackBody" : "body");
        if (params.containsKey(bodyKey) && m instanceof EntityEnclosingMethod) {
            String body = params.get(bodyKey);
            ((EntityEnclosingMethod) m).setRequestEntity(new StringRequestEntity(process(body, templateParams)));
        }

        return m;
    }

    String process(String template, Map<String, String> values) {
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
