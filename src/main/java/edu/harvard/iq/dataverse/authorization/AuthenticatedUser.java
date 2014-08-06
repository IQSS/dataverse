package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
public class AuthenticatedUser implements User, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * In practice, this identifier is a username.
     *
     * @todo Rename this to username? Call it alias?
     */
    @NotNull
    @Column(nullable = false)
    private String identifier;

    @NotNull
    @Column(nullable = false)
    private String displayInfo;

    @Override
    public String getIdentifier() {
        return identifier;
    }

//    public String getUsername() {
//        return identifier;
//    }

    @Override
    public String getDisplayInfo() {
        return displayInfo;
    }

}
