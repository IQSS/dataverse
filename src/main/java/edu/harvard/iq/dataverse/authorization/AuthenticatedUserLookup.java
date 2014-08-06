package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class AuthenticatedUserLookup implements Serializable {

    @Id
    private String persistentUserIdFromIdp;

    @ManyToOne
    @JoinColumn(nullable=false)
    private AuthenticatedUser authenticatedUser;

    public String getPersistentUserIdFromIdp() {
        return persistentUserIdFromIdp;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

}
