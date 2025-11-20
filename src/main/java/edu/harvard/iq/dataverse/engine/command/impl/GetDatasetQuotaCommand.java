package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 * The sole purpose of this command is to check the permissions
 * when it's called by the /api/datasets/.../storage/quota api.
 */
// No @RequiredPermissions defined, dynamic
public class GetDatasetQuotaCommand extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetDatasetQuotaCommand.class.getCanonicalName());
    
    private final Dataset dataset;
    
    public GetDatasetQuotaCommand(DataverseRequest aRequest, Dataset target) {
        super(aRequest, target);
        dataset = target;
    } 
        
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
               
        if (dataset != null && dataset.getStorageQuota() != null) {
            return dataset.getStorageQuota().getAllocation();
        }
        
        return null;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataset.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataset));
    }    
}