package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.license.License;
import java.util.List;

@RequiredPermissions({})
public class UpdateDatasetTypeAvailableLicensesCommand extends AbstractVoidCommand {

    final DatasetType datasetType;
    List<License> licenses;

    public UpdateDatasetTypeAvailableLicensesCommand(DataverseRequest dataverseRequest, DatasetType datasetType, List<License> licenses) {
        super(dataverseRequest, (DvObject) null);
        this.datasetType = datasetType;
        this.licenses = licenses;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Update dataset type links to metadata block command can only be called by superusers.",
                    this, null, null);
        }
        datasetType.setLicenses(licenses);
        ctxt.em().merge(datasetType);
    }

}