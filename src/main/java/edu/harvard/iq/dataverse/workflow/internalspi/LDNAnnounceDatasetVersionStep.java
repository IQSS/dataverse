package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import static edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult.OK;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * A workflow step that generates and sends an LDN Announcement message to the
 * inbox of a configured target. THe initial use case is for Dataverse to
 * anounce new dataset versions to the Harvard DASH preprint repository so that
 * a DASH admin can create a backlink for any dataset versions that reference a
 * DASH deposit or a paper with a DOI where DASH has a preprint copy.
 * 
 * @author qqmyers
 */

public class LDNAnnounceDatasetVersionStep implements WorkflowStep {
    private static final Logger logger = Logger.getLogger(LDNAnnounceDatasetVersionStep.class.getName());
    private final Map<String, String> params;

    private static final String REQUIRED_FIELDS = ":LDNAnnounceRequiredFields";
    private static final String RELATED_PUBLICATION = "publication";
    
    JsonLDTerm publicationIDType = null;
    JsonLDTerm publicationIDNumber = null;
    JsonLDTerm publicationURL = null;

    public LDNAnnounceDatasetVersionStep(Map<String, String> paramSet) {
        params = new HashMap<>(paramSet);
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            // build method
            HttpPost announcement = buildMethod(false, context);
            // execute
            int responseStatus = client.execute(mtd);
            if (responseStatus >= 200 && responseStatus < 300) {
                // HTTP OK range
                return OK;
            } else {
                String responseBody = mtd.getResponseBodyAsString();
                return new Failure("Error communicating with server. Server response: " + responseBody + " ("
                        + responseStatus + ").");
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error communicating with remote server: " + ex.getMessage(), ex);
            return new Failure("Error executing request: " + ex.getLocalizedMessage(),
                    "Cannot communicate with remote server.");
        }
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        HttpClient client = new HttpClient();

        try {
            // build method
            HttpPost post = buildAnnouncement(context);
            if (post != null) {

                // execute
                int responseStatus = client.executeMethod(mtd);
                if (responseStatus < 200 || responseStatus >= 300) {
                    // out of HTTP OK range
                    String responseBody = mtd.getResponseBodyAsString();
                    Logger.getLogger(LDNAnnounceDatasetVersionStep.class.getName()).log(Level.WARNING,
                            "Bad response from remote server while rolling back step: {0}", responseBody);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(LDNAnnounceDatasetVersionStep.class.getName()).log(Level.WARNING,
                    "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }

    HttpPost buildAnnouncement(WorkflowContext ctxt) throws Exception {
        
        //First check that we have what is required 
        DatasetVersion dv = ctxt.getDataset().getReleasedVersion();
        List<DatasetField> dvf = dv.getDatasetFields();
        Map<String, DatasetField> fields = new HashMap<String, DatasetField>();
        String[] requiredFields = ((String) ctxt.getSettings().getOrDefault(REQUIRED_FIELDS, "")).split(",\\s*");
        for(String field:requiredFields) {
            fields.put(field, null);
        }
        Set<String> reqFields = fields.keySet();
        for(DatasetField df: dvf) {
            if (reqFields.contains(df.getDatasetFieldType().getName())) {
                fields.put(df.getDatasetFieldType().getName(), df);
            }
        }
        if(fields.containsValue(null)) {
            logger.fine("DatasetVersion doesn't contain metadata required to trigger announcement");
            return null;
        }
        //We do, so constreuct the json-ld body and method
        
        HttpPost ann = new HttpPost();
        Map<String, String> localContext = new HashMap<String, String>();
        JsonObjectBuilder coarContext = Json.createObjectBuilder();
        Map<Long, JsonObject> emptyCvocMap = new HashMap<Long, JsonObject>();
        for(Entry<String, DatasetField> entry: fields.entrySet()) {
            DatasetField field = entry.getValue();
                DatasetFieldType dft =field.getDatasetFieldType(); 
            String uri = dft.getUri();
            String dfTypeName=entry.getKey(); 
            switch(dfTypeName) {
            case RELATED_PUBLICATION :
                JsonArrayBuilder relArrayBuilder = Json.createArrayBuilder();
                publicationIDType = null;
                publicationIDNumber = null;
                publicationURL = null;
                Collection<DatasetFieldType> childTypes = dft.getChildDatasetFieldTypes();
                for(DatasetFieldType cdft: childTypes) {
                    switch (cdft.getName()) {
                    case "publicationURL":
                        publicationURL = OREMap.getTermFor(dft, cdft);
                        break;
                    case "publicationIDType" :
                        publicationIDType = OREMap.getTermFor(dft, cdft);
                           break;
                    case "publicationIDNumber" : 
                        publicationIDNumber = OREMap.getTermFor(dft, cdft);
break;
                    }
                    
                }
                JsonValue jv = OREMap.getJsonLDForField(field, false, emptyCvocMap, localContext);
                if(jv !=null) {
                    if (jv instanceof JsonArray) {
                JsonArray rels = (JsonArray) jv;
                for(JsonObject jo: rels.getValuesAs(JsonObject.class)) {
                    String id = getBestPubId(jo);
                   
                    
                        relArrayBuilder.add(Json.createObjectBuilder().add("id", number).add("ietf:cite-as", number).add("type","sorg:ScholaryArticle").build());
                    }
                }
                    
                } 
                else { // JsonObject
                    }
                }
                }
                    
                if
                break;
            default:
                JsonValue jv = OREMap.getJsonLDForField(field, false, emptyCvocMap, localContext);
                if(jv!=null) {
                coarContext.add(OREMap.getTermFor(dft).getLabel(), jv);
            }

        }
        dvf.get(0).getDatasetFieldType().getName();
        JsonObjectBuilder job = Json.createObjectBuilder();
        
        job.add("@context", Json.createArrayBuilder().add("https://purl.org/coar/notify").add("https://www.w3.org/ns/activitystreams").build());
        job.add("id", "urn:uuid:" + UUID.randomUUID().toString());
        job.add("actor", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic()).add("name",BrandingUtil.getInstallationBrandName()).add("type","Service"));

        /*
        {
            "@context": [
                "https://purl.org/coar/notify",
                "https://www.w3.org/ns/activitystreams"
            ],
            "id": "urn:uuid:a301c520-f790-4f3d-87b1-a18b2b617683",
            "actor": {
                "id": "http://ec2-35-170-82-7.compute-1.amazonaws.com",
                "name": "Dataverse Repository",
                "type": "Service"
            },
            "context": {
                "IsSupplementTo": [
                    {
                        "id": "https://dashv7-dev.lib.harvard.edu/handle/123456789/34723317",
                        "ietf:cite-as": "https://dashv7-dev.lib.harvard.edu/handle/123456789/34723317",
                        "type": "sorg:ScholarlyArticle"
                    }
                ]
            },
            "object": {
                "id": "http://ec2-35-170-82-7.compute-1.amazonaws.com/dataset.xhtml?persistentId=doi:10.5072/FK2/OMSPHN",
                "ietf:cite-as": "https://doi.org/10.5072/FK2/OMSPHN",
                "sorg:name": "An Interesting Dataset",
                "type": "sorg:Dataset"
            },
            "origin": {
                "id": "http://ec2-35-170-82-7.compute-1.amazonaws.com",
                "inbox": "http://ec2-35-170-82-7.compute-1.amazonaws.com/api/inbox",
                "type": "Service"
            },
            "target": {
                "id": "https://dashv7-dev.lib.harvard.edu",
                "inbox": "https://dashv7-api-dev.lib.harvard.edu/server/ldn/inbox",
                "type": "Service"
            },
            "type": [
                "Announce",
                "coar-notify:ReleaseAction"
            ]
        }
        */
        Map<String,String> templateParams = new HashMap<>();
        templateParams.put( "invocationId", ctxt.getInvocationId() );
        templateParams.put( "dataset.id", Long.toString(ctxt.getDataset().getId()) );
        templateParams.put( "dataset.identifier", ctxt.getDataset().getIdentifier() );
        templateParams.put( "dataset.globalId", ctxt.getDataset().getGlobalIdString() );
        templateParams.put( "dataset.displayName", ctxt.getDataset().getDisplayName() );
        templateParams.put( "dataset.citation", ctxt.getDataset().getCitation() );
        templateParams.put( "minorVersion", Long.toString(ctxt.getNextMinorVersionNumber()) );
        templateParams.put( "majorVersion", Long.toString(ctxt.getNextVersionNumber()) );
        templateParams.put( "releaseStatus", (ctxt.getType()==TriggerType.PostPublishDataset) ? "done":"in-progress" );
        
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
            ((EntityEnclosingMethod)m).setRequestEntity(new StringRequestEntity(process( body, templateParams)));
        }
        
        return m;
    }

    private String getBestPubId(JsonObject jo) {
        String id=null;
        if(jo.containsKey(publicationURL.getLabel()) ) {
            id=jo.getString(publicationURL.getLabel());
        } else if(jo.containsKey(publicationIDType.getLabel())) {
            if((jo.containsKey(publicationIDNumber.getLabel())) ) {
                String number = jo.getString(publicationIDNumber.getLabel());
                
            switch (jo.getString(publicationIDType.getLabel())) {
                case "doi":
                        if(number.startsWith("https://doi.org/")) {
                            id = number;
                        } else if(number.startsWith("doi:")) {
                            id = "https://doi.org/" + doi.substring(4);
                        }
                    
                    break;
                case "DASH-URN":
                   if(number.startsWith("http")) {
                       id=number;
                   }
break;
            }
            }
        }
        return id;
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
