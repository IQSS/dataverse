package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

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
public class JsonPrinterHelper {
    @EJB
    SettingsServiceBean settingsSvc;
    
    @EJB
    DatasetFieldServiceBean datasetFieldSvc;
    
    @EJB
    DataverseFieldTypeInputLevelServiceBean datasetFieldInpuLevelSvc;
    
    @PostConstruct
    public void injectService() {
        JsonPrinter.injectSettingsService(settingsSvc, datasetFieldSvc, datasetFieldInpuLevelSvc);
    }
}
