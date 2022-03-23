package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("admin/feedback")
public class FeedbackApi extends AbstractApiBean {

    @EJB
    DvObjectServiceBean dvObjectSvc;

    @POST
    public Response submitFeedback(JsonObject jsonObject) throws AddressException {
        JsonNumber jsonNumber = jsonObject.getJsonNumber("id");
        DvObject recipient = null;
        if (jsonNumber != null) {
            recipient = dvObjectSvc.findDvObject(jsonNumber.longValue());
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
        List<Feedback> feedbacks = FeedbackUtil.gatherFeedback(recipient, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName);
        feedbacks.forEach((feedback) -> {
            jab.add(feedback.toJsonObjectBuilder());
        });
        return ok(jab);
    }
}
