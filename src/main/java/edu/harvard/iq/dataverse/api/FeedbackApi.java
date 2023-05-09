package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.SendFeedbackDialog;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.MailUtil;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("admin/feedback")
public class FeedbackApi extends AbstractApiBean {

    @EJB MailServiceBean mailService;
    
    /**
     * This method mimics the contact form and sends an email to the contacts of the
     * specified Collection/Dataset/DataFile, optionally ccing the support email
     * address, or to the support email address when there is no target object.
     * 
     * !!!!! This should not be moved outside the /admin path unless/until some form
     * of captcha or other spam-prevention mechanism is added. As is, it allows an
     * unauthenticated user (with access to the /admin api path) to send email from
     * anyone to any contacts in Dataverse. (It also does not do much to validate
     * user input (e.g. to strip potentially malicious html, etc.)!!!!
     **/
    @POST
    public Response submitFeedback(JsonObject jsonObject) throws AddressException {
        JsonNumber jsonNumber = jsonObject.getJsonNumber("targetId");
        DvObject feedbackTarget = null;
        if (jsonNumber != null) {
            feedbackTarget =  dvObjSvc.findDvObject(jsonNumber.longValue());
            if(feedbackTarget==null) {
                return error(Status.BAD_REQUEST, "Feedback target object not found");
            }
        }
        DataverseSession dataverseSession = null;
        String userMessage = jsonObject.getString("body");
        String systemEmail = JvmSettings.SUPPORT_EMAIL.lookupOptional().orElse(settingsSvc.getValueForKey(SettingsServiceBean.Key.SystemEmail));
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
        String userEmail = jsonObject.getString("fromEmail");
        String messageSubject = jsonObject.getString("subject");
        String baseUrl = systemConfig.getDataverseSiteUrl();
        String installationBrandName = BrandingUtil.getInstallationBrandName();
        String supportTeamName = BrandingUtil.getSupportTeamName(systemAddress);
        JsonArrayBuilder jab = Json.createArrayBuilder();
        Feedback feedback = FeedbackUtil.gatherFeedback(feedbackTarget, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, SendFeedbackDialog.ccSupport(feedbackTarget));
        jab.add(feedback.toJsonObjectBuilder());
        mailService.sendMail(feedback.getFromEmail(), feedback.getToEmail(), feedback.getCcEmail(), feedback.getSubject(), feedback.getBody());
        return ok(jab);
    }
}
