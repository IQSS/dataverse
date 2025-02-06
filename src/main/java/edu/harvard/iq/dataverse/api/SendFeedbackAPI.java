package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.engine.command.impl.CheckRateLimitForDatasetFeedbackCommand;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.cache.CacheFactoryBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import jakarta.ejb.EJB;
import jakarta.json.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.text.MessageFormat;
import java.util.logging.Logger;

@Path("sendfeedback")
public class SendFeedbackAPI extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(SendFeedbackAPI.class.getCanonicalName());
    @EJB
    MailServiceBean mailService;
    @EJB
    CacheFactoryBean cacheFactory;
    /**
     * This method mimics the contact form and sends an email to the contacts of the
     * specified Collection/Dataset/DataFile, optionally ccing the support email
     * address, or to the support email address when there is no target object.
     **/
    @POST
    @AuthRequired
    public Response submitFeedback(@Context ContainerRequestContext crc, String jsonString) {
        try {
            JsonObject jsonObject = JsonUtil.getJsonObject(jsonString);
            if (!jsonObject.containsKey("subject") || !jsonObject.containsKey("body")) {
                return badRequest(BundleUtil.getStringFromBundle("sendfeedback.body.error.missingRequiredFields"));
            }

            JsonNumber jsonNumber = jsonObject.containsKey("targetId") ? jsonObject.getJsonNumber("targetId") : null;
            // idtf will hold the "targetId" or the "identifier". If neither is set then this is a general feedback to support
            String idtf = jsonNumber != null ? jsonNumber.toString() : jsonObject.containsKey("identifier") ? jsonObject.getString("identifier") : null;
            DvObject feedbackTarget = null;

            if (jsonNumber != null) {
                feedbackTarget = dvObjSvc.findDvObject(jsonNumber.longValue());
            } else if (idtf != null) {
                if (feedbackTarget == null) {
                    feedbackTarget = dataverseSvc.findByAlias(idtf);
                }
                if (feedbackTarget == null) {
                    feedbackTarget = dvObjSvc.findByGlobalId(idtf, DvObject.DType.Dataset);
                }
                if (feedbackTarget == null) {
                    feedbackTarget = dvObjSvc.findByGlobalId(idtf, DvObject.DType.DataFile);
                }
            }

            // feedbackTarget and idtf are both null this is a support feedback and is ok
            if (feedbackTarget == null && idtf != null) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("sendfeedback.request.error.targetNotFound"));
            }
            // Check for rate limit exceeded.
            if (!cacheFactory.checkRate(getRequestUser(crc), new CheckRateLimitForDatasetFeedbackCommand(null, feedbackTarget))) {
                return error(Response.Status.TOO_MANY_REQUESTS, BundleUtil.getStringFromBundle("sendfeedback.request.rateLimited"));
            }

            DataverseSession dataverseSession = null;
            String userMessage = sanitizeBody(jsonObject.getString("body"));
            InternetAddress systemAddress = mailService.getSupportAddress().orElse(null);
            String userEmail = getEmail(jsonObject, crc);
            String messageSubject = jsonObject.getString("subject");
            String baseUrl = systemConfig.getDataverseSiteUrl();
            String installationBrandName = BrandingUtil.getInstallationBrandName();
            String supportTeamName = BrandingUtil.getSupportTeamName(systemAddress);
            JsonArrayBuilder jab = Json.createArrayBuilder();
            Feedback feedback = FeedbackUtil.gatherFeedback(feedbackTarget, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, SendFeedbackDialog.ccSupport(feedbackTarget));
            jab.add(feedback.toLimitedJsonObjectBuilder());
            mailService.sendMail(feedback.getFromEmail(), feedback.getToEmail(), feedback.getCcEmail(), feedback.getSubject(), feedback.getBody());
            return ok(jab);
        } catch (WrappedResponse resp) {
            return resp.getResponse();
        } catch (JsonException je) {
            return error(Response.Status.BAD_REQUEST, "Invalid JSON; error message: " + je.getMessage());
        }
    }

    private String getEmail(JsonObject jsonObject, ContainerRequestContext crc) throws WrappedResponse {
        String fromEmail = jsonObject.containsKey("fromEmail") ? jsonObject.getString("fromEmail") : "";
        if (fromEmail.isBlank() && crc != null) {
            User user = getRequestUser(crc);
            if (user instanceof AuthenticatedUser) {
                fromEmail = ((AuthenticatedUser) user).getEmail();
            }
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new WrappedResponse(badRequest(BundleUtil.getStringFromBundle("sendfeedback.fromEmail.error.missing")));
        }
        if (!EMailValidator.isEmailValid(fromEmail)) {
            throw new WrappedResponse(badRequest(MessageFormat.format(BundleUtil.getStringFromBundle("sendfeedback.fromEmail.error.invalid"), fromEmail)));
        }
        return fromEmail;
    }
    private String sanitizeBody (String body) throws WrappedResponse {
        // remove malicious html
        String sanitizedBody = body == null ? "" : body.replaceAll("\\<.*?>", "");

        long limit = systemConfig.getContactFeedbackMessageSizeLimit();
        if (limit > 0 && sanitizedBody.length() > limit) {
            throw new WrappedResponse(badRequest(MessageFormat.format(BundleUtil.getStringFromBundle("sendfeedback.body.error.exceedsLength"), sanitizedBody.length(), limit)));
        } else if (sanitizedBody.length() == 0) {
            throw new WrappedResponse(badRequest(BundleUtil.getStringFromBundle("sendfeedback.body.error.isEmpty")));
        }

        return sanitizedBody;
    }
}
