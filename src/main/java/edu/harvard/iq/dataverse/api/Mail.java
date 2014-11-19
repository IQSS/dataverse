/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MailServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 * @author xyang
 * @author Leonid Andreev
 */
@Path("mail")
public class Mail {
    private static final Logger logger = Logger.getLogger(Mail.class.getCanonicalName());
    
    @EJB
    MailServiceBean mailService;
    
    @GET
    @Path("notifications")
    public String sendMail() {
        mailService.bulkSendNotifications();
        return null;
    }
    
}
