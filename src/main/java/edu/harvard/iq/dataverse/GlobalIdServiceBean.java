package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.GlobalIdServiceBean.logger;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface GlobalIdServiceBean {

    static final Logger logger = Logger.getLogger(GlobalIdServiceBean.class.getCanonicalName());

    boolean alreadyExists(DvObject dvo) throws Exception;
    
    boolean alreadyExists(GlobalId globalId) throws Exception;

    boolean registerWhenPublished();
    
    List<String> getProviderInformation();

    String createIdentifier(DvObject dvo) throws Throwable;

    Map<String,String> getIdentifierMetadata(DvObject dvo);

    /**
     * Concatenate the parts that make up a Global Identifier.
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param identifier the local identifier part
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    String getIdentifierForLookup(String protocol, String authority, String identifier);

    String modifyIdentifierTargetURL(DvObject dvo) throws Exception;

    void deleteIdentifier(DvObject dvo) throws Exception;
    
    Map<String, String> getMetadataForCreateIndicator(DvObject dvObject);
    
    Map<String,String> getMetadataForTargetURL(DvObject dvObject);
    
    Map<String,String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier);
    
    DvObject generateIdentifier(DvObject dvObject);
    
    String getIdentifier(DvObject dvObject);
    
    boolean publicizeIdentifier(DvObject studyIn);
    
    static GlobalIdServiceBean getBean(String protocol, CommandContext ctxt) {
        final Function<CommandContext, GlobalIdServiceBean> protocolHandler = BeanDispatcher.DISPATCHER.get(protocol);
        if ( protocolHandler != null ) {
            return protocolHandler.apply(ctxt);
        } else {
            logger.log(Level.SEVERE, "Unknown protocol: {0}", protocol);
            return null;
        }
    }

    static GlobalIdServiceBean getBean(CommandContext ctxt) {
        return getBean(ctxt.settings().getValueForKey(Key.Protocol, ""), ctxt);
    }
    
}

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
    }
}