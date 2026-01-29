package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * No required permissions because we check for superuser status.
 * @param <T>
 */
@RequiredPermissions({})
public class ReservePidCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(ReservePidCommand.class.getCanonicalName());

    private final Dataset dataset;

    public ReservePidCommand(DataverseRequest request, Dataset dataset) {
        super(request, dataset);
        this.dataset = dataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException(BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"),
                    this, Collections.singleton(Permission.EditDataset), dataset);
        }
        registerExternalIdentifier(getDataset(), ctxt, true);
        registerFilePidsIfNeeded(getDataset(), ctxt, true);
        return dataset;
    }

}
