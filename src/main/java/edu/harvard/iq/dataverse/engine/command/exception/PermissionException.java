package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.Permission;
import java.util.Set;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user lacking permissions.
 * 
 * @author michael
 */
public class PermissionException extends CommandException {
	
	private final Set<Permission> required;
	
	public PermissionException(String message, Set<Permission> required ) {
		super(message);
		this.required = required;
	}

	public Set<Permission> getRequiredPermissions() {
		return required;
	}
	
}
