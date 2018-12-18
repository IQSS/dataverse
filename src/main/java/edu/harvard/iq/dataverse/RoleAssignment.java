package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
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
	uniqueConstraints = @UniqueConstraint(columnNames={"assigneeIdentifier","role_id","definitionPoint_id"})
      , indexes = {@Index(columnList="assigneeidentifier")
		, @Index(columnList="definitionpoint_id")
		, @Index(columnList="role_id")}
)
@NamedQueries({
	@NamedQuery( name  = "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.definitionPoint.id=:definitionPointId" ),
    	@NamedQuery( name  = "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId_RoleId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.definitionPoint.id=:definitionPointId and r.role.id=:roleId" ),
	@NamedQuery( name  = "RoleAssignment.listByAssigneeIdentifier",
				 query = "SELECT r FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier" ),
	@NamedQuery( name  = "RoleAssignment.listByAssigneeIdentifiers",
				 query = "SELECT r FROM RoleAssignment r WHERE r.assigneeIdentifier in :assigneeIdentifiers AND r.definitionPoint.id in :definitionPointIds" ),
	@NamedQuery( name  = "RoleAssignment.listByDefinitionPointId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.definitionPoint.id=:definitionPointId" ),
	@NamedQuery( name  = "RoleAssignment.listByRoleId",
				 query = "SELECT r FROM RoleAssignment r WHERE r.role=:roleId" ),
	@NamedQuery( name  = "RoleAssignment.listByPrivateUrlToken",
				 query = "SELECT r FROM RoleAssignment r WHERE r.privateUrlToken=:privateUrlToken" ),
	@NamedQuery( name  = "RoleAssignment.deleteByAssigneeIdentifier_RoleIdDefinition_PointId",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.role.id=:roleId AND r.definitionPoint.id=:definitionPointId"),
        @NamedQuery( name = "RoleAssignment.deleteAllByAssigneeIdentifier",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier"),
        @NamedQuery( name = "RoleAssignment.deleteAllByAssigneeIdentifier_Definition_PointId_RoleType",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.role.id=:roleId and r.definitionPoint.id=:definitionPointId")
})
public class RoleAssignment implements java.io.Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column( nullable=false )
	private String assigneeIdentifier;
		
	@ManyToOne( cascade = {CascadeType.MERGE} )
	@JoinColumn( nullable=false )
	private DataverseRole role;
	
	@ManyToOne( cascade = {CascadeType.MERGE} ) 
	@JoinColumn( nullable=false )
	private DvObject definitionPoint;

    @Column(nullable = true)
    private String privateUrlToken;
	
	public RoleAssignment() {}
		
	public RoleAssignment(DataverseRole aRole, RoleAssignee anAssignee, DvObject aDefinitionPoint, String privateUrlToken) {
        role = aRole;
        assigneeIdentifier = anAssignee.getIdentifier();
        definitionPoint = aDefinitionPoint;
        this.privateUrlToken = privateUrlToken;
    }
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getAssigneeIdentifier() {
        return assigneeIdentifier;
    }

    public void setAssigneeIdentifier(String assigneeIdentifier) {
        this.assigneeIdentifier = assigneeIdentifier;
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

    public String getPrivateUrlToken() {
        return privateUrlToken;
    }

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(role);
		hash = 97 * hash + Objects.hashCode(assigneeIdentifier);
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
		
		return ( Objects.equals(getRole(), other.getRole() )
				 && Objects.equals(getAssigneeIdentifier(), other.getAssigneeIdentifier())
					&& Objects.equals(getDefinitionPoint(), other.getDefinitionPoint()));
		
	}

	@Override
	public String toString() {
		return "RoleAssignment{" + "id=" + id + ", assignee=" + assigneeIdentifier + ", role=" + role + ", definitionPoint=" + definitionPoint + '}';
	}
	
}
