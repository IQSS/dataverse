/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.primefaces.PrimeFaces;

import java.sql.Timestamp;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Date;


/**
 * This bean is mainly for keeping track of which file the user selected to run external tools on.
 * Also for creating an alert across Dataset and DataFile page, and making it easy to get the file-specific handler for a tool.
 * @author madunlap
 */

@ViewScoped
@Named
public class ConfigureFragmentBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ConfigureFragmentBean.class.getName());
    
    private ExternalTool tool = null;
    private Long fileId = null;
    private ExternalToolHandler toolHandler = null;
    private String messageApi = "";
    private Long fileMetadataId = null;
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    
    public String configureExternalAlert() {
        generateApiToken();
        PrimeFaces.current().executeScript("location.reload(true)");
        String httpString = "window.open('" + toolHandler.getToolUrlWithQueryParams()+  "','_blank'" +")";
        PrimeFaces.current().executeScript(httpString);
        return "";
    }    

    /**
     * @param setTool the tool to set
     */
    public void setConfigurePopupTool(ExternalTool setTool) {
        tool = setTool;
    }
    
    /**
     * @return the Tool
     */
    public ExternalTool getConfigurePopupTool() {
        return tool;
    }
    
    public ExternalToolHandler getConfigurePopupToolHandler() {
        if(fileId == null) {
            //on first UI load, method is called before fileId is set. There may be a better way to handle this
            return null;
        }
        if(toolHandler != null) {
            return toolHandler;
        }
        
        //TODO: Pretty sure the below command is doing absolutely nothing --MAD
        datafileService.find(fileId);
        
        ApiToken apiToken = new ApiToken();
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            apiToken = authService.findApiTokenByUser((AuthenticatedUser) user);
        }
        if ((apiToken == null) || (apiToken.getExpireTime().before(new Date()))) {
            messageApi = BundleUtil.getStringFromBundle("configurefragmentbean.apiTokenGenerated");
        } else {
            messageApi = "";
        }

        
        toolHandler = new ExternalToolHandler(tool, datafileService.find(fileId), apiToken, datafileService.findFileMetadata(fileMetadataId), session.getLocaleCode());

        return toolHandler;
    }

    public void  generateApiToken() {

        ApiToken apiToken = new ApiToken();
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            apiToken = authService.findApiTokenByUser((AuthenticatedUser) user);
            if (apiToken == null) {
                //No un-expired token
                apiToken = authService.generateApiTokenForUser(( AuthenticatedUser) user);
                toolHandler.setApiToken(apiToken);
                toolHandler.getToolUrlWithQueryParams();
                userNotificationService.sendNotification((AuthenticatedUser) user, new Timestamp(new Date().getTime()), UserNotification.Type.APIGENERATED, null);
            }
        }

    }
    
    public void setConfigureIds(Long setFileId, Long setFileMetadataId) {

        fileId = setFileId;
        fileMetadataId = setFileMetadataId;
    }

    public String getMessageApi() {
        return messageApi;
    }

}
