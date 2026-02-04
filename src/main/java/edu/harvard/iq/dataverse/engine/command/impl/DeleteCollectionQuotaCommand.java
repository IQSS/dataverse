package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
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
public class DeleteCollectionQuotaCommand  extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(DeleteCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse targetDataverse;
    
    public DeleteCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        targetDataverse = target;
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // first check if  user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException(BundleUtil.getStringFromBundle("dataverse.storage.quota.superusersonly"),
                this,  null, targetDataverse);                
        }
        
        if (targetDataverse == null) {
            throw new IllegalCommandException("", this);
        }
        
        StorageQuota storageQuota = targetDataverse.getStorageQuota();
        
        if (storageQuota != null && storageQuota.getAllocation() != null) {
            ctxt.dataverses().disableStorageQuota(storageQuota);
        } 
        // ... and if no quota was enabled on the collection - nothing to do = success
    }    
}