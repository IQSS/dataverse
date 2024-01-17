package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.GlobalIdServiceBean.logger;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.pidproviders.PermaLinkPidProviderServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface GlobalIdServiceBean {

    static final Logger logger = Logger.getLogger(GlobalIdServiceBean.class.getCanonicalName());

    boolean alreadyRegistered(DvObject dvo) throws Exception;
    
    /**
     * This call reports whether a PID is registered with the external Provider
     * service. For providers like DOIs/Handles with an external service, this call
     * should accurately report whether the PID has been registered in the service.
     * For providers with no external service, the call should return true if the
     * PID is defined locally. If it isn't, these no-service providers need to know
     * whether use case of the caller requires that the returned value should
     * default to true or false - via the noProviderDefault parameter.
     * 
     * @param globalId
     * @param noProviderDefault - when there is no external service, and no local
     *                          use of the PID, this should be returned
     * @return whether the PID should be considered registered or not.
     * @throws Exception
     */
    boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) throws Exception;
    
    boolean registerWhenPublished();
    boolean canManagePID();
    boolean isConfigured();
    
    List<String> getProviderInformation();

    String createIdentifier(DvObject dvo) throws Throwable;

    Map<String,String> getIdentifierMetadata(DvObject dvo);

    String modifyIdentifierTargetURL(DvObject dvo) throws Exception;

    void deleteIdentifier(DvObject dvo) throws Exception;
    
    Map<String, String> getMetadataForCreateIndicator(DvObject dvObject);
    
    Map<String,String> getMetadataForTargetURL(DvObject dvObject);
    
    DvObject generateIdentifier(DvObject dvObject);
    
    String getIdentifier(DvObject dvObject);
    
    boolean publicizeIdentifier(DvObject studyIn);
    
    String generateDatasetIdentifier(Dataset dataset);
    String generateDataFileIdentifier(DataFile datafile);
    boolean isGlobalIdUnique(GlobalId globalId);
    
    String getUrlPrefix();
    String getSeparator();
    
    static GlobalIdServiceBean getBean(String protocol, CommandContext ctxt) {
        final Function<CommandContext, GlobalIdServiceBean> protocolHandler = BeanDispatcher.DISPATCHER.get(protocol);
        if ( protocolHandler != null ) {
            GlobalIdServiceBean theBean = protocolHandler.apply(ctxt);
            if(theBean != null && theBean.isConfigured()) {
                logger.fine("getBean returns " + theBean.getProviderInformation().get(0) + " for protocol " + protocol);
            }
            return theBean;
        } else {
            logger.log(Level.SEVERE, "Unknown protocol: {0}", protocol);
            return null;
        }
    }

    static GlobalIdServiceBean getBean(CommandContext ctxt) {
        return getBean(ctxt.settings().getValueForKey(Key.Protocol, ""), ctxt);
    }
    
    public static Optional<GlobalId> parse(String identifierString) {
        try {
            return Optional.of(PidUtil.parseAsGlobalID(identifierString));
        } catch ( IllegalArgumentException _iae) {
            return Optional.empty();
        }
    }
    
    /** 
     *   Parse a Persistent Id and set the protocol, authority, and identifier
     * 
     *   Example 1: doi:10.5072/FK2/BYM3IW
     *       protocol: doi
     *       authority: 10.5072
     *       identifier: FK2/BYM3IW
     * 
     *   Example 2: hdl:1902.1/111012
     *       protocol: hdl
     *       authority: 1902.1
     *       identifier: 111012
     *
     * @param identifierString
     * @param separator the string that separates the authority from the identifier.
     * @param destination the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     *         {@code null} if parsing failed.
     */
    public GlobalId parsePersistentId(String identifierString);
    public GlobalId parsePersistentId(String protocol, String authority, String identifier);

    
    
    public static boolean isValidGlobalId(String protocol, String authority, String identifier) {
        if (protocol == null || authority == null || identifier == null) {
            return false;
        }
        if(!authority.equals(GlobalIdServiceBean.formatIdentifierString(authority))) {
            return false;
        }
        if (GlobalIdServiceBean.testforNullTerminator(authority)) {
            return false;
        }
        if(!identifier.equals(GlobalIdServiceBean.formatIdentifierString(identifier))) {
            return false;
        }
        if (GlobalIdServiceBean.testforNullTerminator(identifier)) {
            return false;
        }
        return true;
    }
    
    static String formatIdentifierString(String str){
        
        if (str == null){
            return null;
        }
        // remove whitespace, single quotes, and semicolons
        return str.replaceAll("\\s+|'|;","");  
        
        /*
        <   (%3C)
>   (%3E)
{   (%7B)
}   (%7D)
^   (%5E)
[   (%5B)
]   (%5D)
`   (%60)
|   (%7C)
\   (%5C)
+
        */
        // http://www.doi.org/doi_handbook/2_Numbering.html
    }
    
    static boolean testforNullTerminator(String str){
        if(str == null) {
            return false;
        }
        return str.indexOf('\u0000') > 0;
    }
    
    static boolean checkDOIAuthority(String doiAuthority){
        
        if (doiAuthority==null){
            return false;
        }
        
        if (!(doiAuthority.startsWith("10."))){
            return false;
        }
        
        return true;
    }
}


/*
 * ToDo - replace this with a mechanism like BrandingUtilHelper that would read
 * the config and create PidProviders, one per set of config values and serve
 * those as needed. The help has to be a bean to autostart and to hand the
 * required service beans to the PidProviders. That may boil down to just the
 * dvObjectService (to check for local identifier conflicts) since it will be
 * the helper that has to read settings/get systewmConfig values.
 * 
 */

/**
 * Static utility class for dispatching implementing beans, based on protocol and providers.
 * @author michael
 */
class BeanDispatcher {
    static final Map<String, Function<CommandContext, GlobalIdServiceBean>> DISPATCHER = new HashMap<>();

    static {
        DISPATCHER.put("hdl", ctxt->ctxt.handleNet() );
        DISPATCHER.put("doi", ctxt->{
            String doiProvider = ctxt.settings().getValueForKey(Key.DoiProvider, "");
            switch ( doiProvider ) {
                case "EZID": return ctxt.doiEZId();
                case "DataCite": return ctxt.doiDataCite();
                case "FAKE": return ctxt.fakePidProvider();
                default: 
                    logger.log(Level.SEVERE, "Unknown doiProvider: {0}", doiProvider);
                    return null;
            }
        });
        
        DISPATCHER.put(PermaLinkPidProviderServiceBean.PERMA_PROTOCOL, ctxt->ctxt.permaLinkProvider() );
    }
}