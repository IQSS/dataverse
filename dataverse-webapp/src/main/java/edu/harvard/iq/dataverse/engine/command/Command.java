package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Map;
import java.util.Set;

/**
 * Base interface for all commands running on Dataverse.
 *
 * @param <R> The type of result this command returns.
 * @author michael
 */
public interface Command<R> {

    /**
     * Override this method to execute the actual command.
     *
     * @param ctxt the context on which the command work. All dependency injections, if any, should be done using this.
     * @return A result. May be {@code null}
     */
    R execute(CommandContext ctxt);


    /**
     * Retrieves the {@link DvObject}s this command works on. Used by the {@link DataverseEngine}
     * to validate that the user
     * has the permissions required to execute {@code this} command.
     *
     * @return The DvObjects on which the command will work
     */
    Map<String, DvObject> getAffectedDvObjects();


    /**
     * @return The request under which this command is being executed.
     */
    DataverseRequest getRequest();

    /**
     * @return A map of the permissions required for this command
     */
    Map<String, Set<Permission>> getRequiredPermissions();

    default boolean isAllPermissionsRequired() {
        return true;
    }

    String describe();
}
