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

/**
 *
 * @author madunlap
 */

@ViewScoped
@Named
public class ConfigureFragmentBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ConfigureFragmentBean.class.getName());
    
    private String psiPopupUrl = "";
    
    public String psiExternalAlert() { 
        JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("file.psiBroswerinfoDialog.launchMessage.summary"), BundleUtil.getStringFromBundle("file.psiBroswerinfoDialog.launchMessage.details"));
        return "";
    }    

    //MAD This shouldn't be psi specific probably...
    /**
     * @return the psiPopupUrl
     */
    public String getPsiPopupUrl() {
        return psiPopupUrl;
    }

    /**
     * @param psiPopupUrl the psiPopupUrl to set
     */
    public void setPsiPopupUrl(String psiPopupUrl) {
        this.psiPopupUrl = psiPopupUrl;
    }
}
