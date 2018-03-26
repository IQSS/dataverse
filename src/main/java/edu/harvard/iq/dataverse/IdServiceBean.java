package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface IdServiceBean {

    static final Logger logger = Logger.getLogger(IdServiceBean.class.getCanonicalName());
    
    boolean alreadyExists(DvObject dvObject) throws Exception;

    boolean registerWhenPublished();
    
    List<String> getProviderInformation();
    
    String createIdentifier(DvObject dvObject) throws Throwable;
    
    HashMap getIdentifierMetadata(DvObject dvObject);

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
    
    String modifyIdentifierTargetURL(DvObject dvObject) throws Exception;

    void deleteIdentifier(DvObject dvObject) throws Exception;
    
    HashMap getMetadataForCreateIndicator(DvObject dvObject);
    
    HashMap getMetadataForTargetURL(DvObject dvObject);
    
    String getIdentifier(DvObject dvObject);
    
    boolean publicizeIdentifier(DvObject dvObject);

    static IdServiceBean getBean(String protocol, CommandContext ctxt) {
        logger.log(Level.FINE,"getting bean, protocol=" + protocol);
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
        String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        return getBean(protocol, ctxt);
    }
    
     DvObject generateIdentifier(DvObject dvObject);


}
