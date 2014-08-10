package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotNull;

@NamedQueries( {
    @NamedQuery( name="AuthenticatedUserLookup.findByIdp,PersistentId",
                 query="SELECT au FROM AuthenticatedUserLookup au WHERE au.idp=:idp AND au.persistentUserIdFromIdp=:persistentId")
})
@Entity
public class AuthenticatedUserLookup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "Please provide a persistent indentifer by which an user can be looked up.")
    @Column(nullable = false)
    private String persistentUserIdFromIdp;
    
    @NotNull(message = "Please provide a persistent indentifer by which an user can be looked up.")
    @Column(nullable = false)
    private String idp;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser authenticatedUser;

    public String getPersistentUserIdFromIdp() {
        return persistentUserIdFromIdp;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public String getIdp() {
        return idp;
    }

}
