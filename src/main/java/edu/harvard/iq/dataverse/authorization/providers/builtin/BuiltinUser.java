package edu.harvard.iq.dataverse.authorization.providers.builtin;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author xyang
 * @author mbarsinai
 */
@NamedQueries({
		@NamedQuery( name="BuiltinUser.findAll",
				query = "SELECT u FROM BuiltinUser u ORDER BY u.lastName"),
		@NamedQuery( name="BuiltinUser.findByUserame",
				query = "SELECT u FROM BuiltinUser u WHERE u.userName=:username"),
		@NamedQuery( name="BuiltinUser.listByUserNameLike",
				query = "SELECT u FROM BuiltinUser u WHERE u.userName LIKE :userNameLike")
})
@Entity
public class BuiltinUser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Please enter a user name  for your dataverse account.")
    private String userName;

    @NotBlank(message = "Please enter a valid email address.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Please enter your fist name  for your dataverse account.")
    private String firstName;

    @NotBlank(message = "Please enter your last name  for your dataverse account.")
    private String lastName;
    
    private String encryptedPassword;
    private String affiliation;
    private String position;
    
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BuiltinUser)) {
            return false;
        }
        BuiltinUser other = (BuiltinUser) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "BuiltinUser{" + "id=" + id + ", userName=" + userName + ", email=" + email + '}';
	}
   
}
