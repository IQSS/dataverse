package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.PersistentIdentifierServiceBean.logger;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface PersistentIdentifierServiceBean {

    static final Logger logger = Logger.getLogger(PersistentIdentifierServiceBean.class.getCanonicalName());

    boolean alreadyExists(Dataset dataset) throws Exception;

    boolean registerWhenPublished();
    
    List<String> getProviderInformation();

    String createIdentifier(Dataset dataset) throws Throwable;

    Map<String,String> getIdentifierMetadata(Dataset dataset);

    Map<String,String> lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier);

    /**
     * Concatenate the parts that make up a Global Identifier.
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param separator the string that separates authority from local identifier part
     * @param identifier the local identifier part
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    String getIdentifierForLookup(String protocol, String authority, String separator, String identifier);

    String modifyIdentifier(Dataset dataset, Map<String, String> metadata) throws Exception;

    void deleteIdentifier(Dataset datasetIn) throws Exception;

    Map<String,String> getMetadataFromStudyForCreateIndicator(Dataset datasetIn);

    Map<String,String> getMetadataFromDatasetForTargetURL(Dataset datasetIn);

    String getIdentifierFromDataset(Dataset dataset);

    boolean publicizeIdentifier(Dataset studyIn);
    
    static PersistentIdentifierServiceBean getBean(String protocol, CommandContext ctxt) {
        final Function<CommandContext, PersistentIdentifierServiceBean> protocolHandler = BeanDispatcher.DISPATCHER.get(protocol);
        if ( protocolHandler != null ) {
            return protocolHandler.apply(ctxt);
        } else {
            logger.log(Level.SEVERE, "Unknown protocol: {0}", protocol);
            return null;
        }
    }

    static PersistentIdentifierServiceBean getBean(CommandContext ctxt) {
        return getBean(ctxt.settings().getValueForKey(Key.Protocol, ""), ctxt);
    }

}

/**
 * Static utility class for dispatching implementing beans, based on protocol and providers.
 * @author michael
 */
class BeanDispatcher {
    static final Map<String, Function<CommandContext, PersistentIdentifierServiceBean>> DISPATCHER = new HashMap<>();

    static {
        DISPATCHER.put("hdl", ctxt->ctxt.handleNet() );
        DISPATCHER.put("doi", ctxt->{
            String doiProvider = ctxt.settings().getValueForKey(Key.DoiProvider, "");
            switch ( doiProvider ) {
                case "EZID": return ctxt.doiEZId();
                case "DataCite": return ctxt.doiDataCite();
                default: 
                    logger.log(Level.SEVERE, "Unknown doiProvider: {0}", doiProvider);
                    return null;
            }
        });
    }
}