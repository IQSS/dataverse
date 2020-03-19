package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Set;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user lacking permissions.
 *
 * @author michael
 */
public class PermissionException extends CommandException {

    private final Set<Permission> missingPermissions;
    private final DvObject dvObject;

    public PermissionException(String message, Command failedCommand, Set<Permission> missingPermissions, DvObject aDvObject) {
        super(message, failedCommand);
        this.missingPermissions = missingPermissions;
        dvObject = aDvObject;
    }

    public Set<Permission> getMissingPermissions() {
        return missingPermissions;
    }

    public DvObject getDvObject() {
        return dvObject;
    }

}
