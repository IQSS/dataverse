package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * A PrivateUrlUser is virtual in the sense that it does not have a row in the
 * authenticateduser table. It exists so when a Private URL is enabled for a
 * dataset, we can assign a read-only role ("member") to the identifier for the
 * PrivateUrlUser. (We will make no attempt to internationalize the identifier,
 * which is stored in the roleassignment table.)
 */
public class PrivateUrlUser implements User {

    public static final String PREFIX = "!";

    /**
     * In the future, this could probably be dvObjectId rather than datasetId,
     * if necessary. It's really just roleAssignment.getDefinitionPoint(), which
     * is a DvObject.
     */
    private final long datasetId;
    private final boolean anonymizedAccess; 

    public PrivateUrlUser(long datasetId) {
        this(datasetId, false);
    }
    
    public PrivateUrlUser(long datasetId, boolean anonymizedAccess) {
        this.datasetId = datasetId;
        this.anonymizedAccess = anonymizedAccess;
    }

    public long getDatasetId() {
        return datasetId;
    }

    public boolean hasAnonymizedAccess() {
        return anonymizedAccess;
    }
    
    /**
     * By always returning false for isAuthenticated(), we prevent a
     * name from appearing in the corner as well as preventing an account page
     * and MyData from being accessible. The user can still navigate to the home
     * page but can only see published datasets.
     * 
     * @return {@code false}.
     */
    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean isSuperuser() {
        return false;
    }

    @Override
    public String getIdentifier() {
        return PREFIX + datasetId;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        String title = BundleUtil.getStringFromBundle("dataset.privateurl.roleassigeeTitle");
        return new RoleAssigneeDisplayInfo(title, null);
    }



}
