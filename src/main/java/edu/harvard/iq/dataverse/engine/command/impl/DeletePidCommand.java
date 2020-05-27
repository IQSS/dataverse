package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
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
            throw new PermissionException(BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"),
                    this, Collections.singleton(Permission.EditDataset), dataset);
        }

        String nonNullDefaultIfKeyNotFound = "";
        String protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, ctxt);
        try {
            // idServiceBean.deleteIdentifier(dataset); // didn't work
            String baseUrl = System.getProperty("doi.baseurlstringnext");
            String username = System.getProperty("doi.username");
            String password = System.getProperty("doi.password");
            int result = PidUtil.deleteDoi(dataset.getGlobalId().asString(), baseUrl, username, password);
            if (result == 204) {
                // Success! Clear the create time, etc.
                dataset.setGlobalIdCreateTime(null);
                dataset.setIdentifierRegistered(false);
                ctxt.datasets().merge(dataset);
            } else {
                String message = "Unable to delete PID for dataset id " + dataset.getId() + ". Status code:  " + result + ".";
                throw new IllegalCommandException(message, this);
            }
        } catch (IOException ex) {
            String message = "Problem deleting PID for dataset id " + dataset.getId() + ": " + ex.getLocalizedMessage();
            logger.info(message);
            throw new IllegalCommandException(message, this);
        }
    }

}
