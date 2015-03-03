/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MailServiceBean;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 *
 * @author xyang
 * @author Leonid Andreev
 */
@Path("mail")
public class Mail extends AbstractApiBean {
    
    @EJB
    MailServiceBean mailService;
    
    @GET
    @Path("notifications")
    public Response sendMail() {
        mailService.bulkSendNotifications();
        return okResponse("bulk send notification started");
    }
    
}
