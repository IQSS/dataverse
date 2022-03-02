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
import java.util.Optional;
import java.util.Set;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
    @Consumes(MediaType.APPLICATION_JSON + "+ld")
    public Response acceptMessage(String body) {
        IpAddress origin = new DataverseRequest(null, httpRequest).getSourceAddress();
        String whitelist = settingsService.get(SettingsServiceBean.Key.MessageHosts.toString(), "*");
        // Only do something if we listen to this host
        if (whitelist.equals("*") || whitelist.contains(origin.toString())) {
            String citingPID = null;
            String citingType = null;
            boolean sent = false;
            JsonObject jsonld = JSONLDUtil.decontextualizeJsonLD(body);
            if (jsonld == null) {
                throw new BadRequestException("Could not parse message to find acceptable citation link to a dataset.");
            }
            String relationship = "isRelatedTo";
            if (jsonld.containsKey(JsonLDTerm.schemaOrg("identifier").getUrl())) {
                citingPID = jsonld.getJsonObject(JsonLDTerm.schemaOrg("identifier").getUrl()).getString("@id");
                logger.fine("Citing PID: " + citingPID);
                if (jsonld.containsKey("@type")) {
                    citingType = jsonld.getString("@type");
                    if (citingType.startsWith(JsonLDNamespace.schema.getUrl())) {
                        citingType = citingType.replace(JsonLDNamespace.schema.getUrl(), "");
                    }
                    logger.fine("Citing Type: " + citingType);
                    if (jsonld.containsKey(JsonLDTerm.schemaOrg("citation").getUrl())) {
                        JsonObject citation = jsonld.getJsonObject(JsonLDTerm.schemaOrg("citation").getUrl());
                        if (citation != null) {
                            if (citation.containsKey("@type")
                                    && citation.getString("@type").equals(JsonLDTerm.schemaOrg("Dataset").getUrl())
                                    && citation.containsKey(JsonLDTerm.schemaOrg("identifier").getUrl())) {
                                String pid = citation.getString(JsonLDTerm.schemaOrg("identifier").getUrl());
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
                                            .add("@type", citingType).add("relationship", relationship).build();
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
                                // .forEach( au -> userNotificationService.sendNotificationInNewTransaction(au,
                                // timestamp, type, dataset.getLatestVersion().getId()) );

// Subject: <<<Root: You have been assigned a role>>>. Body: Root Support, the Root has just been notified that the http://schema.org/ScholarlyArticle <a href={3}/dataset.xhtml?persistentId={4}>http://ec2-3-236-45-73.compute-1.amazonaws.com</a> cites "<a href={5}>Une Démonstration</a>.<br><br>You may contact us for support at qqmyers@hotmail.com.<br><br>Thank you,<br>Root Support
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
        } else

        {
            logger.info("Ignoring message from IP address: " + origin.toString());
            throw new ForbiddenException("Inbox does not acept messages from this address");
        }

        return

        ok("Message Received");
    }
}
