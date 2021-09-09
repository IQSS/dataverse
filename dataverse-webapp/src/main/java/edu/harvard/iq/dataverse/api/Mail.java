package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author xyang
 * @author Leonid Andreev
 */
@Path("mail")
public class Mail extends AbstractApiBean {

    @Inject
    private ActionLogServiceBean actionLogSvc;

    @GET
    @Path("notifications")
    public Response sendMail() {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "sendMail");
        actionLogSvc.log(alr);
        return ok("bulk send notification is deprecated");
    }

}
