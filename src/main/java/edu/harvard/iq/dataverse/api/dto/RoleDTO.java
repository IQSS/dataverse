package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import java.util.EnumSet;

/**
 *
 * @author michael
 */
public class RoleDTO {
	String alias;
	String name;
	String description;
	String ownerId;
	String[] permissions;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String[] permissions) {
		this.permissions = permissions;
	}

	public DataverseRole updateRoleFromDTO(DataverseRole r) {
		r.setAlias(alias);
		r.setDescription(description);
		r.setName(name);
		r.clearPermissions();
		if (permissions != null) {
			if (permissions.length > 0) {
				if (permissions[0].trim().toLowerCase().equals("all")) {
					r.addPermissions(EnumSet.allOf(Permission.class));
				} else {
					for (String ps : permissions) {
						r.addPermission(Permission.valueOf(ps));
					}
				}
			}
		}
		return r;
	}

	public DataverseRole asRole() {
		return updateRoleFromDTO(new DataverseRole());
	}
	
}
