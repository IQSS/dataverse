package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
				 query = "SELECT r FROM RoleAssignment r WHERE r.role.id=:roleId" ),
	@NamedQuery( name  = "RoleAssignment.listByPrivateUrlToken",
				 query = "SELECT r FROM RoleAssignment r WHERE r.privateUrlToken=:privateUrlToken" ),
	@NamedQuery( name  = "RoleAssignment.deleteByAssigneeIdentifier_RoleIdDefinition_PointId",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.role.id=:roleId AND r.definitionPoint.id=:definitionPointId"),
        @NamedQuery( name = "RoleAssignment.deleteAllByAssigneeIdentifier",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier"),
        @NamedQuery( name = "RoleAssignment.deleteAllByAssigneeIdentifier_Definition_PointId_RoleType",
				 query = "DELETE FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier AND r.role.id=:roleId and r.definitionPoint.id=:definitionPointId")
})
@NamedNativeQueries({
    @NamedNativeQuery(
            name = "RoleAssignment.findAssigneesWithPermissionOnDvObject",
            query = "WITH RECURSIVE owner_hierarchy(id, owner_id, permissionroot) AS ( " +
                    "    SELECT dvo.id, dvo.owner_id, COALESCE(dv.permissionroot, false) " +
                    "    FROM dvobject dvo " +
                    "    LEFT JOIN dataverse dv ON dvo.id = dv.id " +
                    "    WHERE dvo.id = ?2 " +
                    "    UNION ALL " +
                    "    SELECT dvo.id, dvo.owner_id, dv.permissionroot " +
                    "    FROM dvobject dvo " +
                    "    LEFT JOIN dataverse dv ON dvo.id = dv.id " +
                    "    JOIN owner_hierarchy oh ON dvo.id = oh.owner_id " +
                    "    WHERE NOT oh.permissionroot " +
                    ") " +
                    "SELECT DISTINCT ra.assigneeidentifier " +
                    "FROM roleassignment ra " +
                    "JOIN dataverserole dr ON ra.role_id = dr.id " +
                    "JOIN owner_hierarchy oh ON ra.definitionpoint_id = oh.id " +
                    "WHERE get_bit(dr.permissionbits::bit(64), ?1) = '1'",
            resultSetMapping = "AssigneeIdentifierMapping"
        )
})
@SqlResultSetMapping(
        name = "AssigneeIdentifierMapping",
        columns = @ColumnResult(name = "assigneeidentifier")
    )
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
	
    @Column(nullable = true)
    private Boolean privateUrlAnonymizedAccess;
	
	public RoleAssignment() {}
		
	public RoleAssignment(DataverseRole aRole, RoleAssignee anAssignee, DvObject aDefinitionPoint, String privateUrlToken) {
	    this(aRole, anAssignee, aDefinitionPoint, privateUrlToken, false);
	}
	
	public RoleAssignment(DataverseRole aRole, RoleAssignee anAssignee, DvObject aDefinitionPoint, String privateUrlToken, Boolean anonymizedAccess) {
        role = aRole;
        assigneeIdentifier = anAssignee.getIdentifier();
        definitionPoint = aDefinitionPoint;
        this.privateUrlToken = privateUrlToken;
        this.privateUrlAnonymizedAccess=anonymizedAccess;
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

    public boolean isAnonymizedAccess(){
        return (privateUrlAnonymizedAccess==null) ? false: privateUrlAnonymizedAccess;
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
