package edu.harvard.iq.dataverse.pidproviders.perma;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
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
public class UnmanagedPermaLinkPidProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(UnmanagedPermaLinkPidProvider.class.getCanonicalName());
    public static final String ID = "UnmanagedPermaLinkProvider";
    
    public UnmanagedPermaLinkPidProvider() {
        // Also using ID as label
        super(ID, ID, PermaLinkPidProvider.PERMA_PROTOCOL);
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
        return List.of(getId(), "");
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
        /** With a variable separator that could also be empty, there is no way to determine the authority and shoulder for an unknown/unmanaged permalink.
         * Since knowing this split isn't relevant for unmanaged permalinks, the code below just assumes the authority
         * is the first 4 characters and that the separator and the shoulder are empty.
         * If this is found to cause issues, users should be able to use a managed permalink provider as a work-around. The code here could 
         * be changed to allow default lengths for the authority, separator, and shoulder and/or to add a list of known (but unmanaged) authority, separator, shoulder combos.
         */
        if(identifierString.length() < 4) {
            logger.warning("A short unmanaged permalink was found - assuming the authority is empty: " + identifierString);
            return super.parsePersistentId(protocol, "", identifierString);
        }
        return super.parsePersistentId(protocol, identifierString.substring(0, 4), identifierString.substring(4));
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
    
    @Override
    public String getSeparator() {
        return PermaLinkPidProvider.SEPARATOR;
    }
}
