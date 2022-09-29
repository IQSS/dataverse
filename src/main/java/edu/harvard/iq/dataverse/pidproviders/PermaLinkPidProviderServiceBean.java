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

    //ToDo - handle dataset/file defaults for local system
    public static final String PERMA_RESOLVER_URL = System.getProperty("pid.baseurlstring", SystemConfig.getDataverseSiteUrlStatic());
    
    String authority = null; 
    
    @PostConstruct
    private void init() {
        if("PERMA".equals(settingsService.getValueForKey(Key.DoiProvider))){
            authority = settingsService.getValueForKey(Key.Authority);
            isConfigured=true;
        };
        
    }
    
    @Override
    public boolean alreadyExists(GlobalId globalId) throws Exception {
        /*
         * This method is called in cases where the 'right' answer can be true or false:
         * 
         * When called via CreateNewDatasetCommand (direct upload case), we expect
         * 'false' as the response, whereas when called from ImportDatasetCommand or
         * DeleteDataFileCommand, we expect 'true' as a confirmation that the expected
         * PID exists.
         * 
         * This method now checks the stack and can send true/false as expected by the
         * calling command as the right default/normal case.
         *
         * Alternately, this method could check the database as is done in
         * DatasetServiceBean.isIdentifierLocallyUnique() (needs a similar method for
         * DataFiles and could be refactored to only have one query for both).
         */
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        if (walker.walk(this::getCallingClass)) {
            logger.fine("Called from CreateNewDatasetCommand");
            return false;
        }
        return true;
        
    }

    private boolean getCallingClass(Stream<StackFrame> stackFrameStream) {
        /*
         * If/when other cases require a true response from the alreadyExists method,
         * add those class names to the test below.
         */
        return stackFrameStream
                .filter(frame -> frame.getDeclaringClass().getSimpleName()
                        .equals(CreateNewDatasetCommand.class.getSimpleName()))
                .findFirst().map(f -> true).orElse(false);
    }
    

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        ArrayList<String> providerInfo = new ArrayList<>();
        providerInfo.add(PERMA_PROTOCOL);
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

    public GlobalId parsePersistentId(String protocol, String identifierString) {
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
        return new GlobalId(PERMA_PROTOCOL, authority, identifier);
    }
    
    
    @Override
    //No slash between authority and identifier
    public String asString(GlobalId globalId) {
        if (globalId.getProtocol() == null || globalId.getAuthority() == null || globalId.getIdentifier() == null) {
            return "";
        }
        return globalId.getProtocol() + ":" + globalId.getAuthority() + globalId.getIdentifier();
    }
}
