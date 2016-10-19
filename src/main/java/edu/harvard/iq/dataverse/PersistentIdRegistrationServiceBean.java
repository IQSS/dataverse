package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public interface PersistentIdRegistrationServiceBean {

    Logger logger = Logger.getLogger(PersistentIdRegistrationServiceBean.class.getCanonicalName());

    enum Provider {
        EZID {
            public PersistentIdRegistrationServiceBean getBean(CommandContext ctxt) {
                return ctxt.doiEZId();
            }
        }, DataCite {
            public PersistentIdRegistrationServiceBean getBean(CommandContext ctxt) {
                return ctxt.doiDataCite();
            }
        };

        public abstract PersistentIdRegistrationServiceBean getBean(CommandContext ctxt);
    }

    enum Protocol {
        hdl {
            public PersistentIdRegistrationServiceBean getBean(Provider provider, CommandContext ctxt) {
                return ctxt.handleNet();
            }

        }, doi {
            public PersistentIdRegistrationServiceBean getBean(Provider provider, CommandContext ctxt) {
                return provider.getBean(ctxt);
            }

        };

        public abstract PersistentIdRegistrationServiceBean getBean(Provider provider, CommandContext ctxt);
    }

    boolean alreadyExists(Dataset dataset) throws Exception;

    boolean registerWhenPublished();

    String createIdentifier(Dataset dataset) throws Throwable;

    HashMap getIdentifierMetadata(Dataset dataset);

    HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier);

    /**
     * Concatenate the parts that make up a Global Identifier.
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param separator the string that separates authority from local identifier part
     * @param identifier the local identifier part
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    String getIdentifierForLookup(String protocol, String authority, String separator, String identifier);

    String modifyIdentifier(Dataset dataset, HashMap<String, String> metadata) throws Exception;

    void deleteIdentifier(Dataset datasetIn) throws Exception;

    HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn);

    HashMap getMetadataFromDatasetForTargetURL(Dataset datasetIn);

    String getIdentifierFromDataset(Dataset dataset);

    boolean publicizeIdentifier(Dataset studyIn);

    void postDeleteCleanup(final Dataset doomed);

    static PersistentIdRegistrationServiceBean getBean(String protocolString, CommandContext ctxt) {
        logger.log(Level.FINE,"getting bean, protocol=" + protocolString);
        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        try {
            return Protocol.valueOf(protocolString).getBean(Provider.valueOf(doiProvider), ctxt);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unknown doiProvider and/or protocol: " + doiProvider + "  " + protocolString);
            return null;
        }
    }

    static PersistentIdRegistrationServiceBean getBean(CommandContext ctxt) {
        logger.log(Level.FINE,"getting bean with protocol from context");

        String nonNullDefaultIfKeyNotFound = "";
        String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        return getBean(protocol, ctxt);
    }
}
