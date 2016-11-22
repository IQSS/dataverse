package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.ValidateEmail;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author xyang
 * @author mbarsinai
 */
@NamedQueries({
		@NamedQuery( name="BuiltinUser.findAll",
				query = "SELECT u FROM BuiltinUser u ORDER BY u.lastName"),
		@NamedQuery( name="BuiltinUser.findByUserName",
				query = "SELECT u FROM BuiltinUser u WHERE u.userName=:userName"),
		@NamedQuery( name="BuiltinUser.findByEmail",
				query = "SELECT o FROM BuiltinUser o WHERE LOWER(o.email) = LOWER(:email)"),
		@NamedQuery( name="BuiltinUser.listByUserNameLike",
				query = "SELECT u FROM BuiltinUser u WHERE u.userName LIKE :userNameLike")
})
@Entity
@Table(indexes = {@Index(columnList="lastName")})  // for sorting the NamedQuery BuiltinUser.findAll
public class BuiltinUser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{user.enterUsername}")
    @Size(min=2, max=60, message ="{user.usernameLenght}")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-\\.]*", message = "{user.ilegalCharacters}")
    @Column(nullable = false, unique=true)  
    private String userName;

    @NotBlank(message = "{user.invalidEmail}")
    @ValidateEmail(message = "{user.invalidEmail}")
    @Column(nullable = false, unique=true)    
    private String email;

    @NotBlank(message = "{user.firstName}")
    private String firstName;

    @NotBlank(message = "{user.lastName}")
    private String lastName;
    
    private int passwordEncryptionVersion; 
    private String encryptedPassword;
    private String affiliation;
    private String position;
    
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
    
    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getDisplayName(){
        return this.getFirstName() + " " + this.getLastName(); 
    }
    
    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo(getFirstName(), getLastName(), getEmail(), getAffiliation(), getPosition() );
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
		return "BuiltinUser{" + "id=" + id + ", userName=" + userName + ", email=" + email + '}';
	}

    public int getPasswordEncryptionVersion() {
        return passwordEncryptionVersion;
    }

    public void setPasswordEncryptionVersion(int passwordEncryptionVersion) {
        this.passwordEncryptionVersion = passwordEncryptionVersion;
    }
    
}
