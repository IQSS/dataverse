package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 * A somewhat glorified key-value pair, persisted in the database.
 * The value is the {@link AuthenticatedUser}, the internal user representation pointed by the
 * IDP, and the key is a concatenation of the IDP's alias and the user's persistent id
 * within that IDP. These objects may be used both for storage (the full constructor)
 * and retrieval (the idp+id constructor, and then {@link #getLookupKey()}.
 * 
 * @author pdurbin
 * @author michael
 */
@NamedQueries( {
    @NamedQuery( name="AuthenticatedUserLookup.findByLookupKey",
                 query="SELECT au FROM AuthenticatedUserLookup au WHERE au.lookupKey=:lookupKey")
})
@Entity
public class AuthenticatedUserLookup implements Serializable {
    
    private static final String SEPERATOR = "|";
    
    @Id
    String lookupKey;
    
    @NotNull(message = "Please provide a persistent indentifer by which an user can be looked up.")
    @Transient
    private String persistentUserIdFromIdp;
    
    @NotNull(message = "Please provide a persistent indentifer by which an user can be looked up.")
    @Transient
    private String idpAlias;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser authenticatedUser;

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String idp) {
        this.persistentUserIdFromIdp = persistentUserIdFromIdp;
        this.idpAlias = idp;
        updateLookupKey();
    }

    public AuthenticatedUserLookup(String persistentUserIdFromIdp, String idp, AuthenticatedUser authenticatedUser) {
        this.persistentUserIdFromIdp = persistentUserIdFromIdp;
        this.idpAlias = idp;
        this.authenticatedUser = authenticatedUser;
        updateLookupKey();
    }

    /**
     * Constructor for JPA
     */
    public AuthenticatedUserLookup(){}
    
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getPersistentUserIdFromIdp() {
        if ( persistentUserIdFromIdp==null ) parseLookupKey();
        return persistentUserIdFromIdp;
    }

    public void setPersistentUserIdFromIdp(String persistentUserIdFromIdp) {
        this.persistentUserIdFromIdp = persistentUserIdFromIdp;
        updateLookupKey();
    }

    public String getIdpAlias() {
        if ( idpAlias==null ) parseLookupKey();
        return idpAlias;
    }

    public void setIdpAlias(String idp) {
        this.idpAlias = idp;
        updateLookupKey();
    }
    
    private void updateLookupKey() {
        lookupKey = getIdpAlias() + SEPERATOR + getPersistentUserIdFromIdp();
    }
    
    private void parseLookupKey() {
        if ( getLookupKey() != null ) {
            String[] comps = getLookupKey().split("\\|",1);
            setIdpAlias( comps[0] );
            setPersistentUserIdFromIdp( comps[1] );
        }
    }
}
