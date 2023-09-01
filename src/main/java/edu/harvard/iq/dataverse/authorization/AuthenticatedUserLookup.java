package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
@Table(
    uniqueConstraints=
        @UniqueConstraint(columnNames={"persistentuserid", "authenticationproviderid"})
)
@NamedQueries( {
    @NamedQuery( name="AuthenticatedUserLookup.findByAuthPrvID_PersUserId",
                 query="SELECT au FROM AuthenticatedUserLookup au "
                         + "WHERE au.authenticationProviderId=:authPrvId "
                         + "  AND au.persistentUserId=:persUserId "),
    @NamedQuery( name="AuthenticatedUserLookup.findByAuthUser",
                 query="SELECT au FROM AuthenticatedUserLookup au WHERE au.authenticatedUser=:authUser")
})
@Entity
public class AuthenticatedUserLookup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String authenticationProviderId;
    private String persistentUserId;

    @OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(unique=true, nullable=false)
    private AuthenticatedUser authenticatedUser;

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String idp) {
        this( persistentUserIdFromIdp, idp, null );
    }

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String authPrvId, AuthenticatedUser authenticatedUser) {
        this.persistentUserId = persistentUserIdFromIdp;
        this.authenticationProviderId = authPrvId;
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
    
    

}
