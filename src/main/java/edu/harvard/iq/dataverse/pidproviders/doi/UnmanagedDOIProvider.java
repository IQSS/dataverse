package edu.harvard.iq.dataverse.pidproviders.doi;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;

/** This class is just used to parse DOIs that are not managed by any account configured in Dataverse
 * It does not implement any of the methods related to PID CRUD
 * 
 */

public class UnmanagedDOIProvider extends AbstractDOIProvider {

    public static final String ID = "UnmanagedDOIProvider";

    public UnmanagedDOIProvider() {
        //Also using ID as label
        super(ID, ID);
    }

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
    public void deleteIdentifier(DvObject dvObject) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of(getId(), "");
    }

    @Override
    public String getProviderType() {
        return "unamagedDOI";
    }

    // PID recognition
    // Done by AbstractDOIProvider

}
