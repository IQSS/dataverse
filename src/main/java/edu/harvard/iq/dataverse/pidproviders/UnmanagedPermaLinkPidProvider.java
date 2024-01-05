package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import org.apache.commons.lang3.NotImplementedException;

/** This class is just used to parse Handles that are not managed by any account configured in Dataverse
 * It does not implement any of the methods related to PID CRUD
 * 
 */
@Stateless
public class UnmanagedPermaLinkPidProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(UnmanagedPermaLinkPidProvider.class.getCanonicalName());
    private static final String ID = "UnmanagedPermaLinkProvider";
    
    public UnmanagedPermaLinkPidProvider() {
        super(ID, PermaLinkPidProvider.PERMA_PROTOCOL);
        logger.log(Level.FINE, "Constructor");
    }

    @Override
    public boolean canManagePID() {
        return false;
    }

    @Override
    public boolean registerWhenPublished() {
        throw new NotImplementedException();
    }

    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public void deleteIdentifier(DvObject dvObject) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of("UnmanagedPermaLink", "");
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        throw new NotImplementedException();
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        if (!PermaLinkPidProvider.PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        return globalId;
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!PermaLinkPidProvider.PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {
        return SystemConfig.getDataverseSiteUrlStatic()+ "/citation?persistentId=" + PermaLinkPidProvider.PERMA_PROTOCOL + ":";
    }

    @Override
    public String getProviderType() {
        return PermaLinkPidProvider.TYPE;
    }
}
