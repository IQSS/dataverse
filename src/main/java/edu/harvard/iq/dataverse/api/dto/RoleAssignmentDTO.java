package edu.harvard.iq.dataverse.api.dto;

/**
 * Data transfer object for role assignments. Carries the ids.
 * @author michael
 */
public class RoleAssignmentDTO {
	long userId;
	long roleId;
	String userIdentifier;
	String roleAlias;

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

	public String getUserIdentifier() {
		return userIdentifier;
	}

	public void setUserIdentifier(String userIdentifier) {
		this.userIdentifier = userIdentifier;
	}

	public String getRoleAlias() {
		return roleAlias;
	}

	public void setRoleAlias(String roleAlias) {
		this.roleAlias = roleAlias;
	}
	
}
