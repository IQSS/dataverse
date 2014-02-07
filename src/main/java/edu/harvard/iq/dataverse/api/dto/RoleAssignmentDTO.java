package edu.harvard.iq.dataverse.api.dto;

/**
 * Data transfer object for role assignments. Carries the ids.
 * @author michael
 */
public class RoleAssignmentDTO {
	long userId;
	long roleId;
	String username;

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	
}
