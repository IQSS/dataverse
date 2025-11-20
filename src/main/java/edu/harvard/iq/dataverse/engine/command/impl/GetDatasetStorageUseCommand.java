
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 */
@RequiredPermissions(Permission.ManageDatasetPermissions)
public class GetDatasetStorageUseCommand extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetDatasetStorageUseCommand.class.getCanonicalName());
    
    private final Dataset dataset;
    
    public GetDatasetStorageUseCommand(DataverseRequest aRequest, Dataset target) {
        super(aRequest, target);
        dataset = target;
    } 
        
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
               
        if (dataset == null) {
            throw new CommandException("null dataset passed to get storage use command", this);
        }
        return ctxt.storageUse().findStorageSizeByDvContainerId(dataset.getId());        
    }
}