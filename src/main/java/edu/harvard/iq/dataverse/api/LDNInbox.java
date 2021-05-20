package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

import java.util.Arrays;
import java.util.Optional;

import java.io.InputStream;
import java.io.StringReader;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;

@Path("inbox")
public class LDNInbox extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(LDNInbox.class.getName());

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    MailServiceBean mailService;

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
                throw new BadRequestException(
                        "Could not parse message to find acceptable citation link to a dataset.");
            }
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
                                    InternetAddress systemAddress = mailService.getSystemAddress();
                                    String citationMessage = BundleUtil.getStringFromBundle(
                                            "api.ldninbox.citation.alert",
                                            Arrays.asList(
                                                    BrandingUtil.getSupportTeamName(systemAddress),
                                                    BrandingUtil.getInstallationBrandName(), 
                                                    citingType, 
                                                    citingPID,
                                                    systemConfig.getDataverseSiteUrl(),
                                                    dataset.getGlobalId().toString(), 
                                                    dataset.getDisplayName()));
// Subject: <<<Root: You have been assigned a role>>>. Body: Root Support, the Root has just been notified that the http://schema.org/ScholarlyArticle <a href={3}/dataset.xhtml?persistentId={4}>http://ec2-3-236-45-73.compute-1.amazonaws.com</a> cites "<a href={5}>Une Démonstration</a>.<br><br>You may contact us for support at qqmyers@hotmail.com.<br><br>Thank you,<br>Root Support
                                    sent = mailService.sendSystemEmail(
                                            BrandingUtil.getSupportTeamEmailAddress(systemAddress),
                                            BundleUtil.getStringFromBundle("api.ldninbox.citation.subject",
                                                    Arrays.asList(BrandingUtil.getInstallationBrandName())),
                                            citationMessage, true);
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
