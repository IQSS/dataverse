package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import java.util.logging.Logger;

@RequiredPermissions(Permission.ManageDatasetPermissions)
public class GetPrivateUrlCommand extends AbstractCommand<PrivateUrl> {

    private static final Logger logger = Logger.getLogger(GetPrivateUrlCommand.class.getCanonicalName());

    private final Dataset dataset;

    public GetPrivateUrlCommand(DataverseRequest aRequest, Dataset theDataset) {
        super(aRequest, theDataset);
        dataset = theDataset;
    }

    @Override
    public PrivateUrl execute(CommandContext ctxt) throws CommandException {
        logger.fine("GetPrivateUrlCommand called");
        Long datasetId = dataset.getId();
        if (datasetId == null) {
            // Perhaps a dataset is being created in the GUI.
            return null;
        }
        return ctxt.privateUrl().getPrivateUrlFromDatasetId(datasetId);
    }

}
