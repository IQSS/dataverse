package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
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
import javax.validation.constraints.NotNull;

/**
 * A role of a user in a Dataverse. A User may have many roles in a given Dataverse.
 * This is a realization of a Many-to-Many relationship
 * between users and dataverses, with roles as an extra column.
 * @author michael
 */
@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames={"user_id","role_id","definitionPoint_id"}) )
@NamedQueries({
	@NamedQuery( name  = "RoleAssignment.listByUserIdDefinitionPointId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.user.id=:userId AND r.definitionPoint.id=:definitionPointId" ),
	@NamedQuery( name  = "RoleAssignment.listByDefinitionPointId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.definitionPoint.id=:definitionPointId" ),
	@NamedQuery( name  = "RoleAssignment.deleteByUserRoleIdDefinitionPointId",
				 query = "DELETE FROM RoleAssignment r WHERE r.user.id=:userId AND r.role.id=:roleId AND r.definitionPoint.id=:definitionPointId"),
})
public class RoleAssignment implements java.io.Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne( cascade = CascadeType.MERGE )
    @NotNull
	private DataverseUser user;
		
	@ManyToOne( cascade = CascadeType.MERGE )
    @NotNull
	private DataverseRole role;
	
	@ManyToOne( cascade = CascadeType.MERGE ) 
    @NotNull
	private DvObject definitionPoint;
	
	public RoleAssignment() {}
		
	public RoleAssignment(DataverseRole role, DataverseUser user, DvObject definitionPoint) {
		this.role = role;
		this.user = user;
		this.definitionPoint = definitionPoint;
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
		this.user = user;
	}

	public DataverseRole getRole() {
		return role;
	}

	public void setRole(DataverseRole role) {
		this.role = role;
	}

	public DvObject getDefinitionPoint() {
		return definitionPoint;
	}

	public void setDefinitionPoint(DvObject definitionPoint) {
		this.definitionPoint = definitionPoint;
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
		if ( ! (obj instanceof RoleAssignment) ) {
			return false;
		}
		final RoleAssignment other = (RoleAssignment) obj;
		
		return ( Objects.equals(role, other.role)
				 && Objects.equals(user, other.user)
					&& Objects.equals(definitionPoint, other.definitionPoint));
		
	}

	@Override
	public String toString() {
		return "RoleAssignment{" + "id=" + id + ", user=" + user + ", role=" + role + ", definitionPoint=" + definitionPoint + '}';
	}
	
}
