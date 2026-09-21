package edu.harvard.iq.dataverse.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Data transfer object for role assignments.
 * @author michael
 */
@Schema(description = "Role assignment payload identifying the assignee and the role alias.")
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
