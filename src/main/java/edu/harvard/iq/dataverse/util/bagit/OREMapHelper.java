package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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
    
    @PostConstruct
    public void injectService() {
        OREMap.injectSettingsService(settingsSvc, datasetFieldSvc);
    }
}
