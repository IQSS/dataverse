package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 *
 * @author xyang
 * @author Leonid Andreev
 */
@Path("mail")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class Mail extends AbstractApiBean {
    
    @EJB
    MailServiceBean mailService;
    
    @GET
    @Path("notifications")
    @Operation(summary = "Reports deprecated bulk notification status",
            description = "Logs the deprecated bulk notification request and returns a message indicating that bulk notification sending is deprecated.")
    public Response sendMail() {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "sendMail");
       // mailService.bulkSendNotifications();
        actionLogSvc.log(alr);
        return ok("bulk send notification is deprecated");
    }
    
}
