package edu.harvard.iq.dataverse.settings;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import java.io.Serializable;

@ViewScoped
@Named
public class InstallationConfig implements Serializable {

    @EJB
    private InstallationConfigService installationConfigService;
    
    private String supportTeamName;
    
    private String installationName;
    
    
    // -------------------- GETTERS --------------------
    
    public String getSupportTeamName() {
        return supportTeamName;
    }

    public String getInstallationName() {
        return installationName;
    }
    
    // -------------------- LOGIC --------------------
    
    @PostConstruct
    public void postConstruct() {
        this.supportTeamName = installationConfigService.getSupportTeamName();
        this.installationName = installationConfigService.getNameOfInstallation();
    }
    
}
