package edu.harvard.iq.dataverse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import java.io.Serializable;
import java.util.Date;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

@Entity
@Table(name = "role_assignment_audit", indexes = {
    @Index(name = "idx_raa_role_assignment_id", columnList = "role_assignment_id"),
    @Index(name = "idx_raa_action_type", columnList = "action_type"),
    @Index(name = "idx_raa_action_timestamp", columnList = "action_timestamp"),
    @Index(name = "idx_raa_action_by_identifier", columnList = "action_by_identifier"),
    @Index(name = "idx_raa_assignee_identifier", columnList = "assignee_identifier"),
    @Index(name = "idx_raa_role_id", columnList = "role_id"),
    @Index(name = "idx_raa_definition_point_id", columnList = "definition_point_id")
})
@NamedQueries({
    @NamedQuery(name = "RoleAssignmentHistory.findByDefinitionPointId",
        query = "SELECT ra FROM RoleAssignmentHistory ra WHERE ra.definitionPointId = :definitionPointId ORDER BY ra.roleAssignmentId, ra.actionTimestamp DESC"),
    @NamedQuery(name = "RoleAssignmentHistory.findByOwnerId",
    query = "SELECT ra FROM RoleAssignmentHistory ra JOIN DvObject d ON ra.definitionPointId = d.id " +
            "WHERE d.owner.id = :datasetId " +
            "ORDER BY ra.roleAssignmentId, ra.actionTimestamp DESC")
})
public class RoleAssignmentHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "role_assignment_id")
    private Long roleAssignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "action_timestamp", nullable = false)
    private Date actionTimestamp;

    @Column(name = "action_by_identifier", nullable = false)
    private String actionByIdentifier;

    @Column(name = "assignee_identifier", nullable = false)
    private String assigneeIdentifier;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_alias", nullable = false)
    private String roleAlias;

    @Column(name = "definition_point_id", nullable = false)
    private Long definitionPointId;

    @Column(name = "definition_point_identifier", nullable = true)
    private String definitionPointIdentifier;

    public enum ActionType {
        ASSIGN, REVOKE
    }

    // Constructors
    public RoleAssignmentHistory() {
    }

    public RoleAssignmentHistory(RoleAssignment roleAssignment, DataverseRequest request, ActionType actionType) {
        this.roleAssignmentId = roleAssignment.getId();
        this.actionType = actionType;
        this.actionTimestamp = new Date();
        this.actionByIdentifier = request.getUser().getIdentifier();
        this.assigneeIdentifier = roleAssignment.getAssigneeIdentifier();
        this.roleId = roleAssignment.getRole().getId();
        this.roleAlias = roleAssignment.getRole().getAlias();
        this.definitionPointId = roleAssignment.getDefinitionPoint().getId();
        GlobalId globalId = roleAssignment.getDefinitionPoint().getGlobalId();
        if(globalId != null) {
            this.definitionPointIdentifier = roleAssignment.getDefinitionPoint().getGlobalId().asString();
        } else if(roleAssignment.getDefinitionPoint() instanceof Dataverse dv) {
            this.definitionPointIdentifier = dv.getAlias();
        }
    }

    // Getters and setters
    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public Long getRoleAssignmentId() {
        return roleAssignmentId;
    }

    public void setRoleAssignmentId(Long roleAssignmentId) {
        this.roleAssignmentId = roleAssignmentId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Date getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(Date actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public String getActionByIdentifier() {
        return actionByIdentifier;
    }

    public void setActionByIdentifier(String actionByIdentifier) {
        this.actionByIdentifier = actionByIdentifier;
    }

    public String getAssigneeIdentifier() {
        return assigneeIdentifier;
    }

    public void setAssigneeIdentifier(String assigneeIdentifier) {
        this.assigneeIdentifier = assigneeIdentifier;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleAlias() {
        return roleAlias;
    }

    public void setRoleAlias(String roleAlias) {
        this.roleAlias = roleAlias;
    }

    public Long getDefinitionPointId() {
        return definitionPointId;
    }

    public void setDefinitionPointId(Long definitionPointId) {
        this.definitionPointId = definitionPointId;
    }

    public String getDefinitionPointIdentifier() {
        return definitionPointIdentifier;
    }

    public void setDefinitionPointIdentifier(String definitionPointIdentifier) {
        this.definitionPointIdentifier = definitionPointIdentifier;
    }

}