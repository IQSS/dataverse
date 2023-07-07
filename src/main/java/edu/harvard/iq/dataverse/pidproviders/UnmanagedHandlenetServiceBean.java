package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.commons.lang3.NotImplementedException;

/** This class is just used to parse Handles that are not managed by any account configured in Dataverse
 * It does not implement any of the methods related to PID CRUD
 * 
 */
@Stateless
public class UnmanagedHandlenetServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(UnmanagedHandlenetServiceBean.class.getCanonicalName());

    public UnmanagedHandlenetServiceBean() {
        logger.log(Level.FINE, "Constructor");
        configured = true;
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
        return List.of("UnmanagedHandle", "");
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
    public GlobalId parsePersistentId(String pidString) {
        if (pidString.startsWith(HandlenetServiceBean.HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HandlenetServiceBean.HDL_RESOLVER_URL,
                    (HandlenetServiceBean.HDL_PROTOCOL + ":"));
        } else if (pidString.startsWith(HandlenetServiceBean.HTTP_HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HandlenetServiceBean.HTTP_HDL_RESOLVER_URL,
                    (HandlenetServiceBean.HDL_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        if (!HandlenetServiceBean.HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        return globalId;
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!HandlenetServiceBean.HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {
        return HandlenetServiceBean.HDL_RESOLVER_URL;
    }
}
