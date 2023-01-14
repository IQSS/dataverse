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
            String citingPID = null;
            String citingType = null;
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

                citingPID = msgObject.getJsonObject(new JsonLDTerm(ietf, "cite-as").getUrl()).getString("@id");
                logger.fine("Citing PID: " + citingPID);
                if (msgObject.containsKey("@type")) {
                    citingType = msgObject.getString("@type");
                    if (citingType.startsWith(JsonLDNamespace.schema.getUrl())) {
                        citingType = citingType.replace(JsonLDNamespace.schema.getUrl(), "");
                    }
                    if (msgObject.containsKey(JsonLDTerm.schemaOrg("name").getUrl())) {
                        name = msgObject.getString(JsonLDTerm.schemaOrg("name").getUrl());
                    }
                    logger.fine("Citing Type: " + citingType);
                    String contextKey = new JsonLDTerm(activityStreams, "context").getUrl();

                    if (jsonld.containsKey(contextKey)) {
                        JsonObject context = jsonld.getJsonObject(contextKey);
                        for (Map.Entry<String, JsonValue> entry : context.entrySet()) {

                            relationship = entry.getKey().replace("_:", "");
                            // Assuming only one for now - should check for array and loop
                            JsonObject citedResource = (JsonObject) entry.getValue();
                            String pid = citedResource.getJsonObject(new JsonLDTerm(ietf, "cite-as").getUrl())
                                    .getString("@id");
                            if (citedResource.getString("@type").equals(JsonLDTerm.schemaOrg("Dataset").getUrl())) {
                                logger.fine("Raw PID: " + pid);
                                if (pid.startsWith(GlobalId.DOI_RESOLVER_URL)) {
                                    pid = pid.replace(GlobalId.DOI_RESOLVER_URL, GlobalId.DOI_PROTOCOL + ":");
                                } else if (pid.startsWith(GlobalId.HDL_RESOLVER_URL)) {
                                    pid = pid.replace(GlobalId.HDL_RESOLVER_URL, GlobalId.HDL_PROTOCOL + ":");
                                }
                                logger.fine("Protocol PID: " + pid);
                                Optional<GlobalId> id = GlobalId.parse(pid);
                                Dataset dataset = datasetSvc.findByGlobalId(pid);
                                if (dataset != null) {
                                    JsonObject citingResource = Json.createObjectBuilder().add("@id", citingPID)
                                            .add("@type", citingType).add("relationship", relationship)
                                            .add("name", name).build();
                                    StringWriter sw = new StringWriter(128);
                                    try (JsonWriter jw = Json.createWriter(sw)) {
                                        jw.write(citingResource);
                                    }
                                    String jsonstring = sw.toString();
                                    Set<RoleAssignment> ras = roleService.rolesAssignments(dataset);

                                    roleService.rolesAssignments(dataset).stream()
                                            .filter(ra -> ra.getRole().permissions()
                                                    .contains(Permission.PublishDataset))
                                            .flatMap(
                                                    ra -> roleAssigneeService
                                                            .getExplicitUsers(roleAssigneeService
                                                                    .getRoleAssignee(ra.getAssigneeIdentifier()))
                                                            .stream())
                                            .distinct() // prevent double-send
                                            .forEach(au -> {

                                                if (au.isSuperuser()) {
                                                    userNotificationService.sendNotification(au,
                                                            new Timestamp(new Date().getTime()),
                                                            UserNotification.Type.DATASETMENTIONED, dataset.getId(),
                                                            null, null, true, jsonstring);

                                                }
                                            });
                                    sent = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!sent) {
                if (citingPID == null || citingType == null) {
                    throw new BadRequestException(
                            "Could not parse message to find acceptable citation link to a dataset.");
                } else {
                    throw new ServiceUnavailableException(
                            "Unable to process message. Please contact the administrators.");
                }
            }
        } else {
            logger.info("Ignoring message from IP address: " + origin.toString());
            throw new ForbiddenException("Inbox does not acept messages from this address");
        }
        return ok("Message Received");
    }
}
