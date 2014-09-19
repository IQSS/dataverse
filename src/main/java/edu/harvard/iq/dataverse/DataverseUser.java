package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author xyang
 */
@NamedQueries({
		@NamedQuery( name="DataverseUser.findAll",
				query = "SELECT u FROM DataverseUser u ORDER BY u.lastName"),
		@NamedQuery( name="DataverseUser.listByUserNameLike",
				query = "SELECT u FROM DataverseUser u WHERE u.userName LIKE :userNameLike")
})
@Entity
public class DataverseUser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{enterUsernameMsg}")
    private String userName;

    @NotBlank(message = "{enterEmailMsg}")
    @Email(message = "{enterEmailMsg}")
    private String email;

    @NotBlank(message = "{enterFirstNameMsg}")
    private String firstName;

    @NotBlank(message = "{enterLastNameMsg}")
    private String lastName;
    
    private String encryptedPassword;
    private String affiliation;
    private String position;
    
    @OneToMany(mappedBy = "dataverseUser")
    private List<DatasetVersionDatasetUser> datasetDataverseUsers;
    
    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLock> datasetLocks;
	
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
    
    public boolean isGuest() {
        return "__GUEST__".equals( getUserName() );
    }
        
    public List<DatasetVersionDatasetUser> getDatasetDataverseUsers(){
        return datasetDataverseUsers;
    }
    
    public void setDatasetDataverseUsers(List<DatasetVersionDatasetUser> datasetDataverseUsers){
        this.datasetDataverseUsers = datasetDataverseUsers;
    }
    
    public List<DatasetLock> getDatasetLocks() {
        return datasetLocks;
    }

    public void setDatasetLocks(List<DatasetLock> datasetLocks) {
        this.datasetLocks = datasetLocks;
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
        if (!(object instanceof DataverseUser)) {
            return false;
        }
        DataverseUser other = (DataverseUser) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "DataverseUser{" + "id=" + id + ", userName=" + userName + ", email=" + email + '}';
	}
   
}
