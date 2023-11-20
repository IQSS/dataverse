package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 * The command doesn't do much. It's sole purpose is to check the permissions
 * when it's called by the /api/dataverses/.../storage/quota api. 
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class GetCollectionQuotaCommand  extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    
    public GetCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        dataverse = target;
    } 
        
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
               
        if (dataverse != null && dataverse.getStorageQuota() != null) {
            return dataverse.getStorageQuota().getAllocation();
        }
        
        return null;
    }    
}

    
