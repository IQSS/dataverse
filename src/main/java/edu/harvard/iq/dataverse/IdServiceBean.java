package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** TODO Property for Dataset and CommandContext. */
public interface IdServiceBean {

    static final Logger logger = Logger.getLogger(IdServiceBean.class.getCanonicalName());

    boolean alreadyExists(Dataset dataset) throws Exception;

    boolean registerWhenPublished();

    String createIdentifier(Dataset dataset) throws Exception;

    HashMap getIdentifierMetadata(Dataset dataset);

    HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier);

    String getIdentifierForLookup(String protocol, String authority, String separator, String identifier);

    String modifyIdentifier(Dataset dataset, HashMap<String, String> metadata) throws Exception;

    void deleteIdentifier(Dataset datasetIn) throws Exception;

    HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn);

    HashMap getMetadataFromDatasetForTargetURL(Dataset datasetIn);

    String getIdentifierFromDataset(Dataset dataset);

    boolean publicizeIdentifier(Dataset studyIn);

    static IdServiceBean getBean(String protocol, CommandContext ctxt) {
        logger.log(Level.FINE,"getting bean, protocol=" + protocol);
        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);

        if ("hdl".equals(protocol))
            return (ctxt.handleNet());
        else if (protocol.equals("doi"))
            if (doiProvider.equals("EZID"))
                return ctxt.doiEZId();
            else if (doiProvider.equals("DataCite"))
                return ctxt.doiDataCite();
            else logger.log(Level.SEVERE,"Unknown doiProvider: " + doiProvider);
        else logger.log(Level.SEVERE,"Unknown protocol: " + protocol);
        return null;
    }

    static IdServiceBean getBean(CommandContext ctxt) {
        logger.log(Level.FINE,"getting bean with protocol from context");

        String nonNullDefaultIfKeyNotFound = "";
        String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        return getBean(protocol, ctxt);
    }

    String generateYear();

    String generateTimeString();
}
