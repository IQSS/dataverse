/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@NamedQueries(
		@NamedQuery( name="DataverseUser.findAll",
				query = "SELECT u FROM DataverseUser u ORDER BY u.lastName")
)
@Entity
public class DataverseUser implements Serializable {

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
    
	@OneToMany( cascade={CascadeType.MERGE, CascadeType.REMOVE},
			fetch=FetchType.LAZY)
	private Set<UserDataverseAssignedRole> assignedRoles;
	
    private String encryptedPassword;
    private String affiliation;
    private String position;
    private String phone;
    
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
	
	public void registerAssignedRole( UserDataverseAssignedRole udr ) {
		if ( assignedRoles == null ) {
			assignedRoles = new HashSet<>();
		}
		assignedRoles.add(udr);
	}
    
	public void deregisterAssignedRole( UserDataverseAssignedRole udr ) {
		if ( assignedRoles != null ) {
			assignedRoles.remove(udr);
		}
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
        return "edu.harvard.iq.dataverse.DataverseUser[ id=" + id + " ]";
    }
}
