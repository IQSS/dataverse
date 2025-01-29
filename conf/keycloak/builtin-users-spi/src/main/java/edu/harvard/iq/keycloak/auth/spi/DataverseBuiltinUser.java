package edu.harvard.iq.keycloak.auth.spi;

import jakarta.persistence.*;

@NamedQueries({
        @NamedQuery(name = "DataverseUser.findByUsername",
                query = "SELECT u FROM DataverseBuiltinUser u WHERE LOWER(u.username)=LOWER(:username)")
})
@Entity
@Table(name = "builtinuser")
public class DataverseBuiltinUser {
    @Id
    private String id;
    private String username;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
