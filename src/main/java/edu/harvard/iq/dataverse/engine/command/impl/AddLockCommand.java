package edu.harvard.iq.dataverse.engine.command.impl;


import java.util.logging.Logger;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Adds a lock to a dataset.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataset )
public class AddLockCommand extends AbstractCommand<DatasetLock> {

    private static final Logger logger = Logger.getLogger(AddLockCommand.class.getName());
    
    private final Dataset dataset;
    private final DatasetLock lock;
    
    public AddLockCommand(DataverseRequest aRequest, Dataset aDataset, DatasetLock aLock) {
        super(aRequest, aDataset);
        dataset = aDataset;
        lock = aLock;
    }

    @Override
    public DatasetLock execute(CommandContext ctxt) throws CommandException {
        logger.info("executing AddlockCommand");
        ctxt.datasets().addDatasetLock(dataset, lock);
        logger.info("returned from AddDatasetLock" );
        
        return lock;
    }
    
}
