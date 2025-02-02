package edu.harvard.iq.keycloak.auth.spi;

import jakarta.persistence.*;

@NamedQueries({
        @NamedQuery(name = "DataverseAuthenticatedUser.findByEmail",
                query = "select au from DataverseAuthenticatedUser au WHERE LOWER(au.email)=LOWER(:email)"),
        @NamedQuery(name = "DataverseAuthenticatedUser.findByIdentifier",
                query = "select au from DataverseAuthenticatedUser au WHERE LOWER(au.userIdentifier)=LOWER(:identifier)"),
})
@Entity
@Table(name = "authenticateduser")
public class DataverseAuthenticatedUser {
    @Id
    private Integer id;
    private String email;
    private String lastName;
    private String firstName;
    private String userIdentifier;

    public String getEmail() {
        return email;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}
