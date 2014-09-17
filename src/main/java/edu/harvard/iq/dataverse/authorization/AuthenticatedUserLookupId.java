package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;


@Embeddable
public class AuthenticatedUserLookupId implements Serializable {
    String authenticationProviderId;
    String persistentUserId;

    public AuthenticatedUserLookupId(String authPrvId, String userPersistentId) {
        authenticationProviderId = authPrvId;
        persistentUserId = userPersistentId;
    }
    
    public AuthenticatedUserLookupId(){}

    public String getAuthenticationProviderId() {
        return authenticationProviderId;
    }

    public void setAuthenticationProviderId(String authenticationProviderId) {
        this.authenticationProviderId = authenticationProviderId;
    }

    public String getPersistentUserId() {
        return persistentUserId;
    }

    public void setPersistentUserId(String persistentUserId) {
        this.persistentUserId = persistentUserId;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.authenticationProviderId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AuthenticatedUserLookupId other = (AuthenticatedUserLookupId) obj;
        if (!Objects.equals(this.authenticationProviderId, other.authenticationProviderId)) {
            return false;
        }
        if (!Objects.equals(this.persistentUserId, other.persistentUserId)) {
            return false;
        }
        return true;
    }
    
}
