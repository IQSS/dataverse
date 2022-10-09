package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

/*PermaLink provider
 * This is a minimalist permanent ID provider intended for use with 'real' datasets/files where the use case none-the-less doesn't lend itself to the use of DOIs or Handles, e.g.
 * * due to cost
 * * for a catalog/archive where Dataverse has a dataset representing a dataset with DOI/handle stored elsewhere
 * 
 * The initial implementation will mint identifiers locally and will provide the existing page URLs (using the ?persistentID=<id> format). 
 * This will be overridable by a configurable parameter to support use of an external resolver.
 * 
 */
@Stateless
public class PermaLinkPidProviderServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(PermaLinkPidProviderServiceBean.class.getCanonicalName());

    public static final String PERMA_PROTOCOL = "perma";
    public static final String PERMA_PROVIDER_NAME = "PERMA";

    //ToDo - handle dataset/file defaults for local system
    public static final String PERMA_RESOLVER_URL = System.getProperty("pid.baseurlstring", SystemConfig.getDataverseSiteUrlStatic());
    
    String authority = null; 
    private String separator = "";
    
    @PostConstruct
    private void init() {
        if(PERMA_PROVIDER_NAME.equals(settingsService.getValueForKey(Key.DoiProvider))){
            authority = settingsService.getValueForKey(Key.Authority);
            isConfigured=true;
        };
        
    }
    
    @Override
    public String getSeparator() {
        //The perma default
        return separator;
    }
    
    @Override
    public boolean alreadyExists(GlobalId globalId) throws Exception {
        return ! dvObjectService.isGlobalIdLocallyUnique(globalId);
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        ArrayList<String> providerInfo = new ArrayList<>();
        providerInfo.add(PERMA_PROVIDER_NAME);
        providerInfo.add(PERMA_RESOLVER_URL);
        return providerInfo;
    }

    @Override
    public String createIdentifier(DvObject dvo) throws Throwable {
        //Call external resolver and send landing URL?
        //FWIW: Return value appears to only be used in RegisterDvObjectCommand where success requires finding the dvo identifier in this string. (Also logged a couple places).
        return(dvo.getGlobalId().asString());
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvo) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        return getTargetUrl(dvo);
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        // no-op
    }

    @Override
    public boolean publicizeIdentifier(DvObject studyIn) {
      //Call external resolver and send landing URL?
        return true;
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        //ToDo - handle local PID resolver for dataset/file
        if (pidString.startsWith(PERMA_RESOLVER_URL)) {
            pidString = pidString.replace(PERMA_RESOLVER_URL,
                    (PERMA_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        logger.info("Checking identifier: " + identifierString);
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        String identifier = null;
        if (authority != null) {
            if (identifierString.startsWith(authority)) {
                identifier = identifierString.substring(authority.length());
            }
        }
        identifier = GlobalIdServiceBean.formatIdentifierString(identifier);
        if (GlobalIdServiceBean.testforNullTerminator(identifier)) {
            return null;
        }
        return new GlobalId(PERMA_PROTOCOL, authority, identifier, separator, getUrlPrefix(), PERMA_PROVIDER_NAME);
    }
    
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {
        
        return PERMA_RESOLVER_URL + "/citation?persistentId=" + PERMA_PROTOCOL + ":";
    }
}
