package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackInfo;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;

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
import java.util.List;

@Path("admin/feedback")
public class FeedbackApi extends AbstractApiBean {

    @EJB
    DvObjectServiceBean dvObjectSvc;

    @POST
    public Response submitFeedback(JsonObject jsonObject) throws AddressException {
        JsonNumber jsonNumber = jsonObject.getJsonNumber("id");
        DvObject targetObject = null;
        if (jsonNumber != null) {
            targetObject = dvObjectSvc.findDvObject(jsonNumber.longValue());
        }
        InternetAddress systemAddress = new InternetAddress("support@librascholar.edu");
        String rootDataverseName = dataverseSvc.findRootDataverse().getName();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        List<Feedback> feedbacks = FeedbackUtil.gatherFeedback(new FeedbackInfo<>()
                .withFeedbackTarget(targetObject)
                .withUserEmail(jsonObject.getString("fromEmail"))
                .withSystemEmail(systemAddress)
                .withMessageSubject(jsonObject.getString("subject"))
                .withUserMessage(jsonObject.getString("body"))
                .withDataverseSiteUrl(systemConfig.getDataverseSiteUrl())
                .withInstallationBrandName(BrandingUtil.getInstallationBrandName(rootDataverseName))
                .withSupportTeamName(BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName)));
        feedbacks.forEach((feedback) -> {
            jab.add(feedback.toJsonObjectBuilder());
        });
        return ok(jab);
    }
}
