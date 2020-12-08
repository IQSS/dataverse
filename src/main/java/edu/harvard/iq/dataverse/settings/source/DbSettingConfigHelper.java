package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class DbSettingConfigHelper {
    @EJB
    SettingsServiceBean settingsSvc;
    
    @PostConstruct
    public void injectService() {
        DbSettingConfigSource.injectSettingsService(settingsSvc);
    }
}
