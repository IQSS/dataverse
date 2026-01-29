package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 * 
 * A superuser-only command:
 */
@RequiredPermissions({})
public class SetDatasetQuotaCommand   extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(GetDatasetQuotaCommand.class.getCanonicalName());
    
    private final Dataset dataset;
    private final Long allocation; 
    
    public SetDatasetQuotaCommand(DataverseRequest aRequest, Dataset target, Long allocation) {
        super(aRequest, target);
        dataset = target;
        this.allocation = allocation; 
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // Check if user is a superuser:
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException(BundleUtil.getStringFromBundle("dataset.storage.quota.superusersonly"),
                this,  null, dataset);                
        }
        
        if (dataset == null) {
            throw new IllegalCommandException("Must specify valid dataset", this);
        }
        
        if (allocation == null) {
            throw new IllegalCommandException("Must specify valid allocation in bytes", this);
        }
        ctxt.datasets().saveStorageQuota(dataset, allocation);
    }    
}