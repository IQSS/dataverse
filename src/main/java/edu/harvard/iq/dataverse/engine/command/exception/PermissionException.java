package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import java.util.Set;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user lacking permissions.
 * 
 * @author michael
 */
public class PermissionException extends CommandException {
	
	private final Set<Permission> missing;
	private final DvObject dvObject;
	
	public PermissionException(String message, Command failedCommand, Set<Permission> missing, DvObject aDvObject ) {
		super(message, failedCommand);
		this.missing = missing;
		dvObject = aDvObject;
	}

	public Set<Permission> getRequiredPermissions() {
		return missing;
	}

	public DvObject getDvObject() {
		return dvObject;
	}
	
}
