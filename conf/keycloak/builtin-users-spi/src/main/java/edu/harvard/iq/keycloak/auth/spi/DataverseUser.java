package edu.harvard.iq.keycloak.auth.spi;

import jakarta.persistence.*;


@NamedQueries({
        @NamedQuery(name = "DataverseUser.findAll",
                query = "SELECT u FROM DataverseUser u"),
        @NamedQuery(name = "DataverseUser.findByUsername",
                query = "SELECT u FROM DataverseUser u WHERE LOWER(u.username)=LOWER(:username)")
})
@Entity
@Table(name = "builtinuser")
public class DataverseUser {
    @Id
    private String id;
    private String username;
    private int passwordEncryptionVersion;
    private String encryptedPassword;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public int getPasswordEncryptionVersion() {
        return passwordEncryptionVersion;
    }
}