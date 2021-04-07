package edu.harvard.iq.dataverse.export.ddi;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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