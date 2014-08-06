package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
public class AuthenticatedUserLookup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "Please provide a persistent indentifer by which an user can be looked up.")
    @Column(nullable = false)
    private String persistentUserIdFromIdp;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser authenticatedUser;

    public String getPersistentUserIdFromIdp() {
        return persistentUserIdFromIdp;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

}
