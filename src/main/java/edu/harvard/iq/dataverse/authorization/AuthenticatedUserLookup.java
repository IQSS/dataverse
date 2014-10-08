package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

/**
 * A somewhat glorified key-value pair, persisted in the database.
 * The value is the {@link AuthenticatedUser}, the internal user representation pointed by the
 * IDP, and the key is a pair of the authentication provider's alias and the user's persistent id
 * within that authentication provider. These objects may be used both for storage (the full constructor)
 * and retrieval (the idp+id constructor, and then {@link #getLookupKey()}.
 *
 * @author pdurbin
 * @author michael
 */
@NamedQueries( {
    @NamedQuery( name="AuthenticatedUserLookup.findByAuthPrvID_PersUserId",
                 query="SELECT au FROM AuthenticatedUserLookup au "
                         + "WHERE au.id.authenticationProviderId=:authPrvId "
                         + "  AND au.id.persistentUserId=:persUserId ")
})
@Entity
public class AuthenticatedUserLookup implements Serializable {
    
    @EmbeddedId
    AuthenticatedUserLookupId id;

    @OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(unique=true, nullable=false)
    private AuthenticatedUser authenticatedUser;

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String idp) {
        this( persistentUserIdFromIdp, idp, null );
    }

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String authPrvId, AuthenticatedUser authenticatedUser) {
        id = new AuthenticatedUserLookupId();
        id.setAuthenticationProviderId(authPrvId);
        id.setPersistentUserId(persistentUserIdFromIdp);
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Constructor for JPA
     */
    public AuthenticatedUserLookup(){}
    
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public AuthenticatedUserLookupId getId() {
        return id;
    }

    public void setId(AuthenticatedUserLookupId id) {
        this.id = id;
    }
    
}
