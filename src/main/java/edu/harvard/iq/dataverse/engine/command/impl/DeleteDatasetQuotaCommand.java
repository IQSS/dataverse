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
import edu.harvard.iq.dataverse.storageuse.StorageQuota;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 *
 * A superuser-only command:
 */
@RequiredPermissions({})
public class DeleteDatasetQuotaCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(DeleteDatasetQuotaCommand.class.getCanonicalName());
    
    private final Dataset targetDataset;
    
    public DeleteDatasetQuotaCommand(DataverseRequest aRequest, Dataset target) {
        super(aRequest, target);
        targetDataset = target;
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // first check if  user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException(BundleUtil.getStringFromBundle("dataset.storage.quota.superusersonly"),
                this,  null, targetDataset);                
        }
        
        if (targetDataset == null) {
            throw new IllegalCommandException("", this);
        }
        
        StorageQuota storageQuota = targetDataset.getStorageQuota();
        
        if (storageQuota != null && storageQuota.getAllocation() != null) {
            ctxt.dataverses().disableStorageQuota(storageQuota);
        } 
        // ... and if no quota was enabled on the collection - nothing to do = success
    }    
}