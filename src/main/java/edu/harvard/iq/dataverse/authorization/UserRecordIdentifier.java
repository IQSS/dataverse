package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

import java.util.Objects;

/**
 * Identifies a user using two strings:
 * <ul>
 *  <li>Identifier of the user repository in this Dataverse installation</li>
 *  <li>Identifier of the user within that repo</>li>
 * </ul>
 * 
 * @author michael
 */
public class UserRecordIdentifier {
    
    final String repoId;
    final String userIdInRepo;

    public UserRecordIdentifier(String repoId, String userIdInRepo) {
        this.repoId = repoId;
        this.userIdInRepo = userIdInRepo;
    }

    public String getUserRepoId() {
        return repoId;
    }

    public String getUserIdInRepo() {
        return userIdInRepo;
    }

    @Override
    public String toString() {
        return "[UserRecordIdentifier " + repoId + "/" + userIdInRepo + ']';
    }
    
    public AuthenticatedUserLookup createAuthenticatedUserLookup( AuthenticatedUser u ) {
        return new AuthenticatedUserLookup(userIdInRepo, repoId, u);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRecordIdentifier)) return false;
        UserRecordIdentifier that = (UserRecordIdentifier) o;
        return Objects.equals(repoId, that.repoId) && Objects.equals(getUserIdInRepo(), that.getUserIdInRepo());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(repoId, getUserIdInRepo());
    }
}
