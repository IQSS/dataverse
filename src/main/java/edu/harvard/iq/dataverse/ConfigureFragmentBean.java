/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;

/**
 *
 * @author madunlap
 */

@ViewScoped
@Named
public class ConfigureFragmentBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ConfigureFragmentBean.class.getName());
    
    private ExternalTool tool = null;
    
    public String psiExternalAlert() { 
        JH.addMessage(FacesMessage.SEVERITY_WARN, tool.getDisplayName(), BundleUtil.getStringFromBundle("file.configure.launchMessage.details") + " " + tool.getDisplayName());
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
}
