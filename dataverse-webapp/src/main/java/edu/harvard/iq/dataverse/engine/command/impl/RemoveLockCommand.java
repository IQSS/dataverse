package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Removes a lock from the passed dataset;
 * @author michael
 */
@RequiredPermissions( Permission.EditDataset )
public class RemoveLockCommand extends AbstractVoidCommand {

    private final Dataset dataset;
    private final DatasetLock.Reason reason;
    
    public RemoveLockCommand(DataverseRequest aRequest, Dataset aDataset, DatasetLock.Reason aReason) {
        super(aRequest, aDataset);
        dataset = aDataset;
        reason = aReason;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.datasets().removeDatasetLocks(dataset, reason);
    }
    
}

