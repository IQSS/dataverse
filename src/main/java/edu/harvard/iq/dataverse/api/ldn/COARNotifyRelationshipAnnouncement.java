package edu.harvard.iq.dataverse.api.ldn;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import static edu.harvard.iq.dataverse.api.LDNInbox.activityStreams;
import static edu.harvard.iq.dataverse.api.LDNInbox.objectKey;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.BadRequestException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Handles COAR Notify Relationship Announcement messages.
 * Processes relationship announcements between scholarly resources and datasets.
 */
public class COARNotifyRelationshipAnnouncement {

    private static final Logger logger = Logger.getLogger(COARNotifyRelationshipAnnouncement.class.getName());

    private static final String subjectKey = new JsonLDTerm(activityStreams, "subject").getUrl();
    private static final String relationshipKey = new JsonLDTerm(activityStreams, "relationship").getUrl();
    
    private static final boolean notifySuperusersOnly = JvmSettings.COARNOTIFY_RELATIONSHIP_ANNOUNCEMENT_NOTIFY_SUPERUSERS_ONLY
            .lookupOptional(Boolean.class).orElse(false);

    private final DatasetServiceBean datasetService;
    private final UserNotificationServiceBean userNotificationService;
    private final DataverseRoleServiceBean roleService;
    private final RoleAssigneeServiceBean roleAssigneeService;

    public COARNotifyRelationshipAnnouncement(
            DatasetServiceBean datasetService,
            UserNotificationServiceBean userNotificationService,
            DataverseRoleServiceBean roleService,
            RoleAssigneeServiceBean roleAssigneeService) {
        this.datasetService = datasetService;
        this.userNotificationService = userNotificationService;
        this.roleService = roleService;
        this.roleAssigneeService = roleAssigneeService;
    }

    /**
     * Process a COAR Notify Relationship Announcement message.
     *
     * @param msgObject The JSON-LD message object
     * @return true if the message was successfully processed, false otherwise
     */
    public void processMessage(JsonObject msgObject) {
        // Extract subject, object, and relationship from the message
        String subjectId = extractField(msgObject, subjectKey);
        String objectId = extractField(msgObject, objectKey);
        String relationshipId = extractField(msgObject, relationshipKey);

        if (subjectId == null || objectId == null || relationshipId == null) {
            throw new BadRequestException("Can't find the subject, relationship or object in the message - ignoring");
        }

        // Get metadata about the citing resource
        ResourceMetadata metadata = retrieveResourceMetadata(subjectId);

        // Extract relationship label from URI
        String relationship = extractRelationshipLabel(relationshipId);

        // Find the dataset being cited
        Optional<GlobalId> id = PidProvider.parse(objectId);
        if (!id.isPresent()) {
            throw new BadRequestException("Unable to parse relationship object ID as a PID: " + objectId);
        }

        Dataset dataset = datasetService.findByGlobalId(objectId);
        if (dataset == null) {
            logger.info("Didn't find dataset for object ID: " + objectId + " - ignoring");
        }

        // Create the citing resource JSON
        JsonObject citingResource = buildCitingResourceJson(subjectId, relationship, metadata);
        String jsonString = JsonUtil.prettyPrint(citingResource);
        logger.info("Citing resource: " + jsonString);

        // Send notifications to users with publish permissions
        sendNotifications(dataset, jsonString);
    }

    /**
     * Extract a field value from the message object.
     */
    private String extractField(JsonObject msgObject, String key) {
        return msgObject.containsKey(key) ? msgObject.getString(key) : null;
    }

    /**
     * Retrieve metadata about the citing resource using Signposting and DataCite.
     */
    private ResourceMetadata retrieveResourceMetadata(String subjectId) {
        ResourceMetadata metadata = new ResourceMetadata();

        try (CloseableHttpClient client = HttpClients.custom().disableRedirectHandling().build()) {
            logger.info("Getting " + subjectId);

            // Step 1: Initial GET request expecting a 30x redirect
            HttpGet initialGet = new HttpGet(new URI(subjectId));
            initialGet.addHeader("Accept", "application/json");

            CloseableHttpResponse initialResponse = client.execute(initialGet);
            int statusCode = initialResponse.getStatusLine().getStatusCode();

            if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                String location = initialResponse.getFirstHeader("Location").getValue();
                logger.info("Redirecting to: " + location);
                initialResponse.close();

                // Step 2: HEAD request to get Signposting links
                HttpHead headRequest = new HttpHead(location);
                CloseableHttpResponse headResponse = client.execute(headRequest);

                if (headResponse.getStatusLine().getStatusCode() == 200) {
                    String dataciteXmlUrl = extractDataCiteXmlUrl(headResponse);
                    headResponse.close();

                    // Step 3: Retrieve and parse DataCite XML
                    if (dataciteXmlUrl != null) {
                        parseDataCiteXml(dataciteXmlUrl, client, metadata);
                    } else {
                        logger.info("No DataCite XML URL found in Signposting links");
                    }
                } else {
                    logger.info("HEAD request failed with status: " + headResponse.getStatusLine().getStatusCode());
                    headResponse.close();
                }
            } else {
                logger.info("Expected 302/303 redirect but received status: " + statusCode);
                initialResponse.close();
            }

        } catch (Exception e) {
            logger.info("Unable to get metadata from " + subjectId);
            logger.info(e.getLocalizedMessage());
        }

        return metadata;
    }

    /**
     * Extract DataCite XML URL from Signposting Link headers.
     */
    private String extractDataCiteXmlUrl(CloseableHttpResponse headResponse) {
        org.apache.http.Header[] linkHeaders = headResponse.getHeaders("Link");

        for (org.apache.http.Header linkHeader : linkHeaders) {
            String linkValue = linkHeader.getValue();
            if (linkValue.contains("application/vnd.datacite.datacite+xml")) {
                int urlStart = linkValue.indexOf('<');
                int urlEnd = linkValue.indexOf('>');
                if (urlStart != -1 && urlEnd != -1 && urlEnd > urlStart) {
                    String dataciteXmlUrl = linkValue.substring(urlStart + 1, urlEnd);
                    logger.info("Found DataCite XML URL: " + dataciteXmlUrl);
                    return dataciteXmlUrl;
                }
            }
        }

        return null;
    }

    /**
     * Parse DataCite XML to extract title and resource type.
     */
    private void parseDataCiteXml(String dataciteXmlUrl, CloseableHttpClient client, ResourceMetadata metadata) {
        try {
            HttpGet xmlGet = new HttpGet(new URI(dataciteXmlUrl));
            xmlGet.addHeader("Accept", "application/vnd.datacite.datacite+xml, application/xml");

            CloseableHttpResponse xmlResponse = client.execute(xmlGet);

            if (xmlResponse.getStatusLine().getStatusCode() == 200) {
                String xmlContent = EntityUtils.toString(xmlResponse.getEntity(), "UTF-8");
                logger.info("Retrieved DataCite XML");

                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(
                        new java.io.ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

                // Extract title
                org.w3c.dom.NodeList titleNodes = doc.getElementsByTagNameNS("*", "title");
                if (titleNodes.getLength() == 0) {
                    titleNodes = doc.getElementsByTagName("title");
                }

                if (titleNodes.getLength() > 0) {
                    metadata.name = titleNodes.item(0).getTextContent();
                    logger.info("Extracted title from DataCite XML: " + metadata.name);
                } else {
                    logger.info("No title element found in DataCite XML");
                }

                // Extract resource type
                org.w3c.dom.NodeList resourceTypeNodes = doc.getElementsByTagNameNS("*", "resourceType");
                if (resourceTypeNodes.getLength() == 0) {
                    resourceTypeNodes = doc.getElementsByTagName("resourceType");
                }

                if (resourceTypeNodes.getLength() > 0) {
                    org.w3c.dom.Element resourceTypeElement = (org.w3c.dom.Element) resourceTypeNodes.item(0);
                    String resourceTypeGeneral = resourceTypeElement.getAttribute("resourceTypeGeneral");
                    if (resourceTypeGeneral != null && !resourceTypeGeneral.isEmpty()) {
                        metadata.itemType = resourceTypeGeneral.toLowerCase();
                        logger.info("Extracted resource type: " + metadata.itemType);
                    }
                }

            } else {
                logger.info("Failed to retrieve DataCite XML. Status: " + xmlResponse.getStatusLine().getStatusCode());
            }

            xmlResponse.close();

        } catch (Exception parseException) {
            logger.warning("Failed to parse DataCite XML: " + parseException.getMessage());
        }
    }

    /**
     * Extract relationship label from relationship URI.
     * Assumes the label exists after a # character.
     */
    private String extractRelationshipLabel(String relationshipId) {
        int index = relationshipId.indexOf("#");
        logger.info("Found # at " + index + " in " + relationshipId);
        return relationshipId.substring(index + 1);
    }

    /**
     * Build the JSON object representing the citing resource.
     */
    private JsonObject buildCitingResourceJson(String subjectId, String relationship, ResourceMetadata metadata) {
        JsonObjectBuilder citingResourceBuilder = Json.createObjectBuilder()
                .add("@id", subjectId)
                .add("relationship", relationship);

        if (metadata.name != null && !metadata.name.isBlank()) {
            citingResourceBuilder.add("name", metadata.name);
        }

        if (metadata.itemType != null && !metadata.itemType.isBlank()) {
            citingResourceBuilder.add("@type", metadata.itemType);
        }

        return citingResourceBuilder.build();
    }

    /**
     * Send notifications to users with publish permissions on the dataset.
     */
    private void sendNotifications(Dataset dataset, String jsonString) {
        roleService.rolesAssignments(dataset).stream()
                .filter(assignment -> assignment.getRole().permissions().contains(Permission.PublishDataset))
                .flatMap(ra -> roleAssigneeService
                        .getExplicitUsers(roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier())).stream())
                .distinct() // prevent double-send
                .forEach(au -> {

                    if (!notifySuperusersOnly || au.isSuperuser()) {
                        userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()),
                                UserNotification.Type.DATASETMENTIONED, dataset.getId(), null, null, true, jsonString);

                    }
                });
    }

    /**
     * Inner class to hold resource metadata.
     */
    private static class ResourceMetadata {
        String name;
        String itemType;
    }
}
