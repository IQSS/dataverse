package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.GlobalIdServiceBean.logger;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PermaLinkPidProviderServiceBean;
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

    String modifyIdentifierTargetURL(DvObject dvo) throws Exception;

    void deleteIdentifier(DvObject dvo) throws Exception;
    
    Map<String, String> getMetadataForCreateIndicator(DvObject dvObject);
    
    Map<String,String> getMetadataForTargetURL(DvObject dvObject);
    
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
    String generateDatasetIdentifier(Dataset dataset);
    String generateDataFileIdentifier(DataFile datafile);
    boolean isGlobalIdUnique(GlobalId globalId);
    
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