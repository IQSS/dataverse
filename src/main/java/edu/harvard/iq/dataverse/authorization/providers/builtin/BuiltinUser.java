package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.validation.ValidateUserName;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetData;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *
 * @author xyang
 * @author mbarsinai
 */
@NamedQueries({
		@NamedQuery( name="BuiltinUser.findAll",
				query = "SELECT u FROM BuiltinUser u ORDER BY u.userName"),
		@NamedQuery( name="BuiltinUser.findByUserName",
				query = "SELECT u FROM BuiltinUser u WHERE LOWER(u.userName)=LOWER(:userName)"),
		@NamedQuery( name="BuiltinUser.listByUserNameLike",
				query = "SELECT u FROM BuiltinUser u WHERE u.userName LIKE :userNameLike")
})
@Entity
@Table(indexes = {@Index(columnList="userName")})  // for sorting the NamedQuery BuiltinUser.findAll
public class BuiltinUser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ValidateUserName
    @Column(nullable = false, unique=true)  
    private String userName;
    
    private int passwordEncryptionVersion; 

    @OneToOne(mappedBy = "builtinUser", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private PasswordResetData passwordResetData;

    private String encryptedPassword;

    /**
     * These attributes are kept as transients for legacy purposes, namely to ease
     * the creation of users via API with serialization
     * 
     * We do not provide getters because the only time these need to be gotten
     * is not individually
     */
    @Transient
    private String email;
    @Transient
    private String firstName;
    @Transient
    private String lastName;
    @Transient
    private String affiliation;
    @Transient
    private String position;
    
    @Deprecated()
    public String getEmail() {
        return email;
    }
    @Deprecated()
    public void setEmail(String email) {
       this.email = email;
    }
    @Deprecated()
    public String getFirstName() {
       return firstName;
    }
    @Deprecated()
    public void setFirstName(String firstName) {
       this.firstName = firstName;
    }
    @Deprecated()
    public String getLastName() {
       return lastName;
    }
    @Deprecated()
    public void setLastName(String lastName) {
       this.lastName = lastName;
    }
    @Deprecated()
    public String getAffiliation() {
       return affiliation;
    }
    @Deprecated()
    public void setAffiliation(String affiliation) {
       this.affiliation = affiliation;
    }
    @Deprecated()
    public String getPosition() {
       return position;
    }
    @Deprecated()
    public void setPosition(String position) {
       this.position = position;
    }
    
    public void updateEncryptedPassword( String encryptedPassword, int algorithmVersion ) {
        setEncryptedPassword(encryptedPassword);
        setPasswordEncryptionVersion(algorithmVersion);
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    /**
     * JPA-use only. Humans should call {@link #updateEncryptedPassword(java.lang.String, int)}
     * and update the password and the algorithm at the same time.
     * 
     * @param encryptedPassword
     * @deprecated
     */
    @Deprecated()
    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof BuiltinUser)) {
            return false;
        }
        BuiltinUser other = (BuiltinUser) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
            return "BuiltinUser{" + "id=" + id + ", userName=" + userName + '}';
    }

    public int getPasswordEncryptionVersion() {
        return passwordEncryptionVersion;
    }

    public void setPasswordEncryptionVersion(int passwordEncryptionVersion) {
        this.passwordEncryptionVersion = passwordEncryptionVersion;
    }
    
    /**
     * This only exists at this point to ease creation of users via API.
     * Previously we stored more information in the BuiltInUser, but this was
     * removed and only stored with AuthenticatedUser.
     * We use this along with the transient BuiltinUser attributes to gather
     * needed data for user creation.
     * 
     * @deprecated
     */
    @Deprecated()
    public AuthenticatedUserDisplayInfo getDisplayInfoForApiCreation() {
        return new AuthenticatedUserDisplayInfo(firstName, lastName, email, affiliation, position );
    }
}
