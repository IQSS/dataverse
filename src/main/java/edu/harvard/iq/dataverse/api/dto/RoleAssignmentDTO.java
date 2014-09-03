package edu.harvard.iq.dataverse.api.dto;

/**
 * Data transfer object for role assignments.
 * @author michael
 */
public class RoleAssignmentDTO {
	String assignee;
	String role;

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
