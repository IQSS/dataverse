package edu.harvard.iq.dataverse;

import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A role of a user in a Dataverse. A User may have many roles in a given Dataverse.
 * This is a realization of a Many-to-Many relationship
 * between users and dataverses, with roles as an extra column.
 * @author michael
 */
@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames={"user_id","role_id"}) )
@NamedQueries({
	@NamedQuery( name  = "UserDataverseAssignedRole.findByUserIdDataverseId",
				 query = "SELECT r FROM UserDataverseAssignedRole r WHERE r.user.id=:userId AND r.role.owner.id=:dataverseId" ),
	@NamedQuery( name  = "UserDataverseAssignedRole.findRoleByUserIdDataverseId",
				 query = "SELECT r.role FROM UserDataverseAssignedRole r WHERE r.user.id=:userId AND r.role.owner.id=:dataverseId" ),
	@NamedQuery( name  = "UserDataverseAssignedRole.deleteByUserIdRoleId",
				 query = "DELETE FROM UserDataverseAssignedRole r WHERE r.user.id=:userId AND r.role.id=:roleId"),
	@NamedQuery( name  = "UserDataverseAssignedRole.deleteByUserIdDataverseId",
				 query = "DELETE FROM UserDataverseAssignedRole r WHERE r.user.id=:userId AND r.role.owner.id=:dataverseId")
})
public class UserDataverseAssignedRole implements java.io.Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne( cascade = CascadeType.MERGE )
	private DataverseUser user;
		
	@ManyToOne( cascade = CascadeType.MERGE )
	private DataverseRole role;
	
	public UserDataverseAssignedRole() {}
		
	public UserDataverseAssignedRole(DataverseRole role, DataverseUser user) {
		this.role = role;
		this.user = user;
		updateLinks();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public DataverseUser getUser() {
		return user;
	}

	public void setUser(DataverseUser user) {
		if ( this.user != null ) {
			this.user.deregisterAssignedRole(this);
		}
		this.user = user;
		if ( this.user != null ) {
			this.user.registerAssignedRole(this);
		}
	}

	public DataverseRole getRole() {
		return role;
	}

	public void setRole(DataverseRole role) {
		if ( this.role != null ) {
			this.role.deregisterAssignedRole(this);
		}
		this.role = role;
		if ( this.role != null ) {
			this.role.registerAssignedRole(this);
		}
	}
	
	/**
	 * De-registers the role assignment for the user and role.
	 */
	public void revoke() {
		if ( role != null ) role.deregisterAssignedRole(this);
		if ( user != null ) user.deregisterAssignedRole(this);
	}
	
	private void updateLinks() {
		if ( role != null )
			role.registerAssignedRole(this);
		
		if ( user != null )
			user.registerAssignedRole(this);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(role);
		hash = 97 * hash + Objects.hashCode(user);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ( ! (obj instanceof UserDataverseAssignedRole) ) {
			return false;
		}
		final UserDataverseAssignedRole other = (UserDataverseAssignedRole) obj;
		
		return ( Objects.equals(role, other.role)
				 && Objects.equals(user, other.user) );
		
	}
}
