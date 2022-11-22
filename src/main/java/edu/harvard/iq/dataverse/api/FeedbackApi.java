package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("admin/feedback")
public class FeedbackApi extends AbstractApiBean {

    @POST
    public Response submitFeedback(JsonObject jsonObject) throws AddressException {
        JsonNumber jsonNumber = jsonObject.getJsonNumber("id");
        DvObject feedbackTarget = null;
        if (jsonNumber != null) {
            feedbackTarget =  dvObjSvc.findDvObject(jsonNumber.longValue());
        }
        DataverseSession dataverseSession = null;
        String userMessage = jsonObject.getString("body");
        String systemEmail = "support@librascholar.edu";
        InternetAddress systemAddress = new InternetAddress(systemEmail);
        String userEmail = jsonObject.getString("fromEmail");
        String messageSubject = jsonObject.getString("subject");
        String baseUrl = systemConfig.getDataverseSiteUrl();
        String installationBrandName = BrandingUtil.getInstallationBrandName();
        String supportTeamName = BrandingUtil.getSupportTeamName(systemAddress);
        JsonArrayBuilder jab = Json.createArrayBuilder();
        boolean ccSupport=feedbackTarget!=null &&settingsSvc.isTrueForKey(SettingsServiceBean.Key.CCSupportOnContactEmails, false);
        Feedback feedback = FeedbackUtil.gatherFeedback(feedbackTarget, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, ccSupport);
        jab.add(feedback.toJsonObjectBuilder());
        
        return ok(jab);
    }
}
