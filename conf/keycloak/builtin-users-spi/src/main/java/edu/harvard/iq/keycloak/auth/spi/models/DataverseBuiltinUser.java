package edu.harvard.iq.keycloak.auth.spi.models;

import jakarta.persistence.*;

@NamedQueries({
        @NamedQuery(name = "DataverseBuiltinUser.findByUsername",
                query = "SELECT u FROM DataverseBuiltinUser u WHERE LOWER(u.username)=LOWER(:username)")
})
@Entity
@Table(name = "builtinuser")
public class DataverseBuiltinUser {
    @Id
    private Integer id;

    private String username;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
