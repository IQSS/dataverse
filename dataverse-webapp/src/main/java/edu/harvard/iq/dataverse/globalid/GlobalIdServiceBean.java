package edu.harvard.iq.dataverse.globalid;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public interface GlobalIdServiceBean {

    Logger logger = Logger.getLogger(GlobalIdServiceBean.class.getCanonicalName());

    boolean alreadyExists(DvObject dvo) throws Exception;

    boolean alreadyExists(GlobalId globalId) throws Exception;

    boolean registerWhenPublished();

    List<String> getProviderInformation();

    String createIdentifier(DvObject dvo) throws Throwable;

    Map<String, String> getIdentifierMetadata(DvObject dvo);

    /**
     * Concatenate the parts that make up a Global Identifier.
     *
     * @param protocol   the identifier system, e.g. "doi"
     * @param authority  the namespace that the authority manages in the identifier system
     * @param identifier the local identifier part
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    String getIdentifierForLookup(String protocol, String authority, String identifier);

    String modifyIdentifierTargetURL(DvObject dvo) throws Exception;

    void deleteIdentifier(DvObject dvo) throws Exception;

    Map<String, String> getMetadataForCreateIndicator(DvObject dvObject);

    Map<String, String> getMetadataForTargetURL(DvObject dvObject);

    Map<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier);

    DvObject generateIdentifier(DvObject dvObject);

    String getIdentifier(DvObject dvObject);

    boolean publicizeIdentifier(DvObject studyIn);

    /**
     * @deprecated use {@link GlobalIdServiceBeanResolver} instead
     */
    @Deprecated
    static GlobalIdServiceBean getBean(String protocol, CommandContext ctxt) {
        return ctxt.globalIdServiceBeanResolver().resolve(protocol);
    }

    /**
     * @deprecated use {@link GlobalIdServiceBeanResolver} instead
     */
    static GlobalIdServiceBean getBean(CommandContext ctxt) {
        return getBean(ctxt.settings().getValueForKey(Key.Protocol), ctxt);
    }

}
