package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * No required permissions because we check for superuser status.
 */
@RequiredPermissions({})
public class DeletePidCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(ReservePidCommand.class.getCanonicalName());

    private final Dataset dataset;

    public DeletePidCommand(DataverseRequest request, Dataset dataset) {
        super(request, dataset);
        this.dataset = dataset;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException(BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"), this,
                    Collections.singleton(Permission.EditDataset), dataset);
        }

        PidProvider pidProvider = PidUtil.getPidProvider(dataset.getGlobalId().getProviderId());

        try {
            pidProvider.deleteIdentifier(dataset);
            // Success! Clear the create time, etc.
            dataset.setGlobalIdCreateTime(null);
            dataset.setIdentifierRegistered(false);
            ctxt.datasets().merge(dataset);
        } catch (Exception ex) {
            String message = BundleUtil.getStringFromBundle("pids.deletePid.failure",
                    Arrays.asList(dataset.getGlobalId().asString(), ex.getLocalizedMessage()));
            logger.info(message);
            throw new IllegalCommandException(message, this);
        }
    }

}
