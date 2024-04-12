package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Timestamp;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

@Path("inbox")
public class LDNInbox extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(LDNInbox.class.getName());

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    MailServiceBean mailService;

    @EJB
    UserNotificationServiceBean userNotificationService;

    @EJB
    DataverseRoleServiceBean roleService;

    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @Context
    protected HttpServletRequest httpRequest;

    @POST
    @Path("/")
    @Consumes("application/ld+json, application/json-ld")
    public Response acceptMessage(String body) {
        try {
        IpAddress origin = new DataverseRequest(null, httpRequest).getSourceAddress();
        String whitelist = settingsService.get(SettingsServiceBean.Key.LDNMessageHosts.toString(), "");
        // Only do something if we listen to this host
        if (whitelist.equals("*") || whitelist.contains(origin.toString())) {
            boolean sent = false;

            JsonObject jsonld = null;
            jsonld = JSONLDUtil.decontextualizeJsonLD(body);
            if (jsonld == null) {
                // Kludge - something about the coar notify URL causes a
                // LOADING_REMOTE_CONTEXT_FAILED error in the titanium library - so replace it
                // and try with a local copy
                body = body.replace("\"https://purl.org/coar/notify\"",
                        "{\n" + "                \"@vocab\": \"http://purl.org/coar/notify_vocabulary/\",\n"
                                + "                \"ietf\": \"http://www.iana.org/assignments/relation/\",\n"
                                + "                \"coar-notify\": \"http://purl.org/coar/notify_vocabulary/\",\n"
                                + "                \"sorg\": \"http://schema.org/\",\n"
                                + "                \"ReviewAction\": \"coar-notify:ReviewAction\",\n"
                                + "                \"EndorsementAction\": \"coar-notify:EndorsementAction\",\n"
                                + "                \"IngestAction\": \"coar-notify:IngestAction\",\n"
                                + "                \"ietf:cite-as\": {\n" + "                \"@type\": \"@id\"\n"
                                + "                }}");
                jsonld = JSONLDUtil.decontextualizeJsonLD(body);
            }
            if (jsonld == null) {
                throw new BadRequestException("Could not parse message to find acceptable citation link to a dataset.");
            }
            //To Do - lower level for PR
            logger.info(JsonUtil.prettyPrint(jsonld));
            //String relationship = "isRelatedTo";
            String name = null;
            String itemType = null;
            JsonLDNamespace activityStreams = JsonLDNamespace.defineNamespace("as",
                    "https://www.w3.org/ns/activitystreams#");
            //JsonLDNamespace ietf = JsonLDNamespace.defineNamespace("ietf", "http://www.iana.org/assignments/relation/");
            String objectKey = new JsonLDTerm(activityStreams, "object").getUrl();
            if (jsonld.containsKey(objectKey)) {
                JsonObject msgObject = jsonld.getJsonObject(objectKey);
                if (new JsonLDTerm(activityStreams, "Relationship").getUrl().equals(msgObject.getString("@type"))) {
                    // We have a relationship message - need to get the subject and object and
                    // relationship type
                    String subjectId = msgObject.getJsonObject(new JsonLDTerm(activityStreams, "subject").getUrl()).getString("@id");
                    String objectId = msgObject.getJsonObject(new JsonLDTerm(activityStreams, "object").getUrl()).getString("@id");
                    // Best-effort to get name by following redirects and looing for a 'name' field in the returned json
                    try (CloseableHttpClient client = HttpClients.createDefault()) {
                        logger.info("Getting " + subjectId);
                        HttpGet get = new HttpGet(new URI(subjectId));
                        get.addHeader("Accept", "application/json");
                        
                        int statusCode=0;
                        do {
                            CloseableHttpResponse response = client.execute(get);
                            statusCode = response.getStatusLine().getStatusCode();
                            switch (statusCode) {
                            case 302:
                            case 303:
                                String location=response.getFirstHeader("location").getValue();
                                logger.info("Redirecting to: " + location);
                                get = new HttpGet(location);
                                get.addHeader("Accept", "application/json");
                                
                                break;
                            case 200: 
                                String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
                                logger.info("Received: " + responseString);
                                JsonObject job = JsonUtil.getJsonObject(responseString);
                                name = job.getString("name", null);
                                itemType = job.getString("type", null);
                                break;
                            default:
                                logger.info("Received " + statusCode + " when accessing " + subjectId);
                            }
                        } while(statusCode == 302);
                    } catch (Exception e) {
                        logger.info("Unable to get name from " + subjectId);
                        logger.info(e.getLocalizedMessage());
                    }
                    String relationshipId = msgObject
                            .getJsonObject(new JsonLDTerm(activityStreams, "relationship").getUrl()).getString("@id");
                    if (subjectId != null && objectId != null && relationshipId != null) {
                        // Strip the URL part from a relationship ID/URL assuming a usable label exists
                        // after a # char. Default is to use the whole URI.
                        int index = relationshipId.indexOf("#");
                        logger.info("Found # at " + index + " in " + relationshipId);
                        String relationship = relationshipId.substring(index + 1);
                        // Parse the URI as a PID and see if this Dataverse instance has this dataset
                        Optional<GlobalId> id = PidProvider.parse(objectId);
                        if (id.isPresent()) {
                            //ToDo - avoid reparsing GlobalId by making a findByGlobalId(GlobalId) method?
                            Dataset dataset = datasetSvc.findByGlobalId(objectId);
                            if (dataset != null) {
                                JsonObjectBuilder citingResourceBuilder = Json.createObjectBuilder().add("@id", subjectId)
                                        .add("relationship", relationship);
                                if(name!=null && !name.isBlank()) {
                                    citingResourceBuilder.add("name",name);
                                }
                                if(itemType!=null && !itemType.isBlank()) {
                                    citingResourceBuilder.add("@type",itemType.substring(0,1).toUpperCase() + itemType.substring(1));
                                }
                                JsonObject citingResource  = citingResourceBuilder.build();
                                StringWriter sw = new StringWriter(128);
                                try (JsonWriter jw = Json.createWriter(sw)) {
                                    jw.write(citingResource);
                                }
                                String jsonstring = sw.toString();
                                logger.info("Storing: " + jsonstring);
                                //Set<RoleAssignment> ras = roleService.rolesAssignments(dataset);

                                roleService.rolesAssignments(dataset).stream()
                                        .filter(ra -> ra.getRole().permissions().contains(Permission.PublishDataset))
                                        .flatMap(ra -> roleAssigneeService
                                                .getExplicitUsers(
                                                        roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()))
                                                .stream())
                                        .distinct() // prevent double-send
                                        .forEach(au -> {

                                            if (au.isSuperuser()) {
                                                userNotificationService.sendNotification(au,
                                                        new Timestamp(new Date().getTime()),
                                                        UserNotification.Type.DATASETMENTIONED, dataset.getId(), null,
                                                        null, true, jsonstring);

                                            }
                                        });
                                sent = true;
                            }
                        } else {
                            logger.info("Didn't find dataset");
                            // We don't have a dataset corresponding to the object of the relationship - do
                            // nothing
                        }

                    } else {
                        // Can't get subject, relationship, object from message
                        logger.info("Can't find the subject, relationship or object in the message - ignoring");

                    }
                } else {
                
                // This isn't a Relationship announcement message - ignore
                logger.info("This is not a relationship announcement - ignoring message of type "
                        + msgObject.getJsonString("@type"));
                }
            }

            if (!sent) {
                throw new ServiceUnavailableException("Unable to process message. Please contact the administrators.");
            }
        } else {
            logger.info("Ignoring message from IP address: " + origin.toString());
            throw new ForbiddenException("Inbox does not acept messages from this address");
        }
        } catch (Throwable t) {
            logger.severe(t.getLocalizedMessage());
            t.printStackTrace();
            
            throw t;
        }
        return ok("Message Received");
    }
}
