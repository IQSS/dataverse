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
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

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
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authService;
    
    public String configureExternalAlert() { 
        JH.addMessage(FacesMessage.SEVERITY_WARN, tool.getDisplayName(), BundleUtil.getStringFromBundle("file.configure.launchMessage.details") + " " + tool.getDisplayName() + ".");
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
        
        toolHandler = new ExternalToolHandler(tool, datafileService.find(fileId), apiToken);
        return toolHandler;
    }
    
    public void setConfigureFileId(Long setFileId)
    {
        fileId = setFileId;
    }
}
