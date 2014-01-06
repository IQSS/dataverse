package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import java.util.Set;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user lacking permissions.
 * 
 * @author michael
 */
public class PermissionException extends CommandException {
	
	private final Set<Permission> required;
	private final Dataverse dataverse;
	
	public PermissionException(String message, Command failedCommand, Set<Permission> required, Dataverse aDataverse ) {
		super(message, failedCommand);
		this.required = required;
		dataverse = aDataverse;
	}

	public Set<Permission> getRequiredPermissions() {
		return required;
	}

	public Dataverse getDataverse() {
		return dataverse;
	}
	
}
