
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.api.ldn.COARNotifyRelationshipAnnouncement;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

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

    public static final JsonLDNamespace activityStreams = JsonLDNamespace.defineNamespace("as",
            "https://www.w3.org/ns/activitystreams#");
    public static final String objectKey = new JsonLDTerm(activityStreams, "object").getUrl();

    @POST
    @Path("/")
    @Consumes("application/ld+json, application/json-ld")
    @Operation(summary = "Accepts a Linked Data Notification",
            description = "Accepts a whitelisted Linked Data Notification inbox message and dispatches supported relationship announcements.")
    @RequestBody(description = "Linked Data Notification message body encoded as JSON-LD.")
    public Response acceptMessage(@RequestBody(description = "Linked Data Notification message body encoded as JSON-LD.") String body) {
        try {
            IpAddress origin = new DataverseRequest(null, httpRequest).getSourceAddress();
            String allowedIPs = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS.lookupOptional().orElse("");
            
            // Only process messages from whitelisted hosts
            if (!allowedIPs.equals("*") && !allowedIPs.contains(origin.toString())) {
                logger.fine("Ignoring message from IP address: " + origin.toString());
                throw new ForbiddenException("The LDN Inbox does not accept messages from this address");
            }

            // Parse JSON-LD message
            JsonObject jsonld = parseJsonLD(body);
            if (jsonld == null) {
                throw new BadRequestException("Could not parse JSON message.");
            }

            logger.fine(JsonUtil.prettyPrint(jsonld));

            // Process message based on type
            processMessage(jsonld);

            return ok("Message Received");

        } catch (Throwable t) {
            logger.warning(t.getLocalizedMessage());
            if(logger.isLoggable(Level.FINE)) {
                t.printStackTrace();
            }
            throw t;
        }
    }

    /**
     * Parse JSON-LD message with fallback for COAR Notify context issues.
     */
    private JsonObject parseJsonLD(String body) {
        JsonObject jsonld = JSONLDUtil.decontextualizeJsonLD(body);
        
        if (jsonld == null) {
            // The COAR Notify URL has many redirects which cause a
            // LOADING_REMOTE_CONTEXT_FAILED error in the titanium library - so replace it
            // with the contents of the final redirect (current as of 10/29/2025)
            // and try again
            body = body.replace("\"https://purl.org/coar/notify\"",
                    """
                    {
                        "@vocab": "http://purl.org/coar/notify_vocabulary/",
                        "ietf": "http://www.iana.org/assignments/relation/",
                        "coar-notify": "http://purl.org/coar/notify_vocabulary/",
                        "sorg": "http://schema.org/",
                        "ReviewAction": "coar-notify:ReviewAction",
                        "EndorsementAction": "coar-notify:EndorsementAction",
                        "IngestAction": "coar-notify:IngestAction",
                        "ietf:cite-as": {
                        "@type": "@id"
                        }
                    }""");
            jsonld = JSONLDUtil.decontextualizeJsonLD(body);
        }
        
        return jsonld;
    }

    /**
     * Process the message based on its type.
     * Returns true if the message was successfully processed.
     */
    private void processMessage(JsonObject jsonld) throws WebApplicationException {
        if (!jsonld.containsKey(objectKey)) {
            throw new BadRequestException("Message does not contain an 'object' key - ignoring");
        }

        JsonObject msgObject = jsonld.getJsonObject(objectKey);
        String messageType = msgObject.getString("@type", "");

        switch (messageType) {
            case "https://www.w3.org/ns/activitystreams#Relationship":
                handleRelationshipAnnouncement(msgObject);
                break;
            
            default:
                throw new ServiceUnavailableException("Unsupported message type: " + messageType + " - ignoring");
        }
    }

    /**
     * Handle COAR Notify Relationship Announcement messages.
     */
    private void handleRelationshipAnnouncement(JsonObject msgObject) {
        COARNotifyRelationshipAnnouncement handler = new COARNotifyRelationshipAnnouncement(
                datasetService,
                userNotificationService,
                roleService,
                roleAssigneeService
        );
        
        handler.processMessage(msgObject);
    }
}
