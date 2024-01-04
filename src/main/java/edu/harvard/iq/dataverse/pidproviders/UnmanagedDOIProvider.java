package edu.harvard.iq.dataverse.pidproviders;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang3.NotImplementedException;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;

/** This class is just used to parse DOIs that are not managed by any account configured in Dataverse
 * It does not implement any of the methods related to PID CRUD
 * 
 */

public class UnmanagedDOIProvider extends DOIProvider {

    public UnmanagedDOIProvider(String providerAuthority, String providerShoulder, String identifierGenerationStyle,
            String datafilePidFormat) {
        super(providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat);
    }

    private static final Logger logger = Logger.getLogger(UnmanagedDOIProvider.class.getCanonicalName());

    @Override
    public boolean canManagePID() {
        return false;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) {
        throw new NotImplementedException();
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Exception {
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
    public void deleteIdentifier(DvObject dvObject) throws IOException, HttpException {
        throw new NotImplementedException();
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of("UnmanagedDOIProvider", "");
    }

    @Override
    public String getProviderType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    // PID recognition
    // Done by DOIProvider

}
