package edu.harvard.iq.dataverse.util.json;

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
public class JsonPrinterHelper {
    @EJB
    SettingsServiceBean settingsSvc;
    
    @PostConstruct
    public void injectService() {
        JsonPrinter.injectSettingsService(settingsSvc);
    }
}
