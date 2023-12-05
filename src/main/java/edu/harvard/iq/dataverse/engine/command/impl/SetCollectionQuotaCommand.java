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
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 *
 * A superuser-only command:
 */
@RequiredPermissions({})
public class SetCollectionQuotaCommand  extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(GetCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    private final Long allocation; 
    
    public SetCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target, Long allocation) {
        super(aRequest, target);
        dataverse = target;
        this.allocation = allocation; 
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // Check if user is a superuser:
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException(BundleUtil.getStringFromBundle("dataverse.storage.quota.superusersonly"),
                this,  null, dataverse);                
        }
        
        if (dataverse == null) {
            throw new IllegalCommandException("Must specify valid collection", this);
        }
        
        if (allocation == null) {
            throw new IllegalCommandException("Must specify valid allocation in bytes", this);
        }
        
        ctxt.dataverses().saveStorageQuota(dataverse, allocation);
    }    
}
