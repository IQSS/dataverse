package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.Collections;

/**
 * Command to update an existing Dataverse attribute.
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseAttributeCommand extends AbstractCommand<Dataverse> {

    private static final String ATTRIBUTE_ALIAS = "alias";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_DESCRIPTION = "description";
    private static final String ATTRIBUTE_AFFILIATION = "affiliation";
    private static final String ATTRIBUTE_FILE_PIDS_ENABLED = "filePIDsEnabled";
    private static final String ATTRIBUTE_REQUIRE_FILES_TO_PUBLISH_DATASET = "requireFilesToPublishDataset";
    private final Dataverse dataverse;
    private final String attributeName;
    private final Object attributeValue;

    public UpdateDataverseAttributeCommand(DataverseRequest request, Dataverse dataverse, String attributeName, Object attributeValue) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        switch (attributeName) {
            case ATTRIBUTE_ALIAS:
            case ATTRIBUTE_NAME:
            case ATTRIBUTE_DESCRIPTION:
            case ATTRIBUTE_AFFILIATION:
                setStringAttribute(attributeName, attributeValue);
                break;
            case ATTRIBUTE_REQUIRE_FILES_TO_PUBLISH_DATASET:
            case ATTRIBUTE_FILE_PIDS_ENABLED:
                setBooleanAttribute(ctxt, true);
                break;
            default:
                throw new IllegalCommandException("'" + attributeName + "' is not a supported attribute", this);
        }

        return ctxt.engine().submit(new UpdateDataverseCommand(dataverse, null, null, getRequest(), null));
    }

    /**
     * Helper method to set a string attribute.
     *
     * @param attributeName  The name of the attribute.
     * @param attributeValue The value of the attribute (must be a String).
     * @throws IllegalCommandException if the provided attribute value is not of String type.
     */
    private void setStringAttribute(String attributeName, Object attributeValue) throws IllegalCommandException {
        if (!(attributeValue instanceof String stringValue)) {
            throw new IllegalCommandException("'" + attributeName + "' requires a string value", this);
        }

        switch (attributeName) {
            case ATTRIBUTE_ALIAS:
                dataverse.setAlias(stringValue);
                break;
            case ATTRIBUTE_NAME:
                dataverse.setName(stringValue);
                break;
            case ATTRIBUTE_DESCRIPTION:
                dataverse.setDescription(stringValue);
                break;
            case ATTRIBUTE_AFFILIATION:
                dataverse.setAffiliation(stringValue);
                break;
            default:
                throw new IllegalCommandException("Unsupported string attribute: " + attributeName, this);
        }
    }

    /**
     * Helper method to handle boolean attributes.
     *
     * @param ctxt The command context.
     * @param adminOnly True if this attribute can only be modified by an Administrator
     * @throws PermissionException if the user doesn't have permission to modify this attribute.
     */
    private void setBooleanAttribute(CommandContext ctxt, boolean adminOnly) throws CommandException {
        if (adminOnly && !getRequest().getUser().isSuperuser()) {
            throw new PermissionException("You must be a superuser to change this setting",
                    this, Collections.singleton(Permission.EditDataset), dataverse);
        }

        if (!(attributeValue instanceof Boolean)) {
            throw new IllegalCommandException("'" + attributeName + "' requires a boolean value", this);
        }
        switch (attributeName) {
            case ATTRIBUTE_FILE_PIDS_ENABLED:
                if (!ctxt.settings().isTrueForKey(SettingsServiceBean.Key.AllowEnablingFilePIDsPerCollection, false)) {
                    throw new PermissionException("Changing File PID policy per collection is not enabled on this server",
                            this, Collections.singleton(Permission.EditDataset), dataverse);
                }
                dataverse.setFilePIDsEnabled((Boolean) attributeValue);
            case ATTRIBUTE_REQUIRE_FILES_TO_PUBLISH_DATASET:
                dataverse.setRequireFilesToPublishDataset((Boolean) attributeValue);
                break;
            default:
                throw new IllegalCommandException("Unsupported boolean attribute: " + attributeName, this);
        }
    }
}
