package edu.harvard.iq.dataverse.export.ddi;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the DdiExportUtil once it's ready.
     */
    @Singleton
    @Startup
    public class DdiExportUtilHelper {

        @EJB SettingsServiceBean settingsSvc;
        
        @PostConstruct
        public void injectService() {
            DdiExportUtil.injectSettingsService(settingsSvc);
        }
    }