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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
            String relationship = "isRelatedTo";
            String name = null;
            JsonLDNamespace activityStreams = JsonLDNamespace.defineNamespace("as",
                    "https://www.w3.org/ns/activitystreams#");
            JsonLDNamespace ietf = JsonLDNamespace.defineNamespace("ietf", "http://www.iana.org/assignments/relation/");
            String objectKey = new JsonLDTerm(activityStreams, "object").getUrl();
            if (jsonld.containsKey(objectKey)) {
                JsonObject msgObject = jsonld.getJsonObject(objectKey);
                if (new JsonLDTerm(activityStreams, "Relationship").getUrl().equals(msgObject.getJsonString("@type"))) {
                    // We have a relationship message - need to get the subject and object and
                    // relationship type
                    String subjectId = msgObject.getJsonObject(new JsonLDTerm(activityStreams, "subject").getUrl())
                            .getString("@id");
                    String objectId = msgObject.getJsonObject(new JsonLDTerm(activityStreams, "object").getUrl())
                            .getString("@id");
                    String relationshipId = msgObject
                            .getJsonObject(new JsonLDTerm(activityStreams, "relationship").getUrl()).getString("@id");
                    if (subjectId != null && objectId != null && relationshipId != null) {
                        // Strip the URL part from a relationship ID/URL assuming a usable label exists
                        // after a # char. Default is to use the whole URI.
                        relationship = relationshipId.substring(relationship.indexOf("#") + 1);
                        // Parse the URI as a PID and see if this Dataverse instance has this dataset
                        String pid = GlobalId.getInternalFormOfPID(objectId);
                        Optional<GlobalId> id = GlobalId.parse(pid);
                        if (id.isPresent()) {
                            Dataset dataset = datasetSvc.findByGlobalId(pid);
                            if (dataset != null) {
                                JsonObject citingResource = Json.createObjectBuilder().add("@id", subjectId)
                                        .add("relationship", relationship).build();
                                StringWriter sw = new StringWriter(128);
                                try (JsonWriter jw = Json.createWriter(sw)) {
                                    jw.write(citingResource);
                                }
                                String jsonstring = sw.toString();
                                Set<RoleAssignment> ras = roleService.rolesAssignments(dataset);

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
                            // We don't have a dataset corresponding to the object of the relationship - do
                            // nothing
                        }

                    } else {
                        // Can't get subject, relationship, object from message
                        logger.info("Can't find the subject, relationship or object in the message - ignoring");

                    }
                } else {
                }
                // This isn't a Relationship announcement message - ignore
                logger.info("This is not a relationship announcement - ignoring message of type "
                        + msgObject.getJsonString("@type"));

            }

            if (!sent) {
                throw new ServiceUnavailableException("Unable to process message. Please contact the administrators.");
            }
        } else {
            logger.info("Ignoring message from IP address: " + origin.toString());
            throw new ForbiddenException("Inbox does not acept messages from this address");
        }
        return ok("Message Received");
    }
}
