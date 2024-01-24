package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * This is a small helper bean 
 * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
 * settings service into the OREMap once it's ready.
 */
@Singleton
@Startup
public class OREMapHelper {
    @EJB
    SettingsServiceBean settingsSvc;
    
    @EJB
    DatasetFieldServiceBean datasetFieldSvc;
    
    @EJB
    SystemConfig systemConfig;
    
    @PostConstruct
    public void injectService() {
        OREMap.injectServices(settingsSvc, datasetFieldSvc, systemConfig);
    }
}
