package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface IdServiceBean {

    static final Logger logger = Logger.getLogger(IdServiceBean.class.getCanonicalName());

    boolean alreadyExists(Dataset dataset) throws Exception;

    boolean registerWhenPublished();
    
    List<String> getProviderInformation();

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

    static IdServiceBean getBean(String protocol, CommandContext ctxt) {
        logger.log(Level.FINE, "getting bean, protocol={0}", protocol);
        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        if (protocol == null){
            protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        }
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
        String protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        return getBean(protocol, ctxt);
    }


}
