package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
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
 * The command doesn't do much. It's sole purpose is to check the permissions
 * when it's called by the /api/dataverses/.../storage/quota api. 
 */
// @RequiredPermissions - none defined, dynamic
public class GetCollectionQuotaCommand  extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    private final boolean inherited;
    
    public GetCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target, boolean inherited) {
        super(aRequest, target);
        dataverse = target;
        this.inherited = inherited;
    } 
        
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
               
        if (dataverse != null) {

            if (dataverse.getStorageQuota() != null) {
                return dataverse.getStorageQuota().getAllocation();
            } else if (inherited) {
                Dataverse uptree = dataverse; 
                while (uptree.getStorageQuota() == null && uptree.getOwner() != null) {
                    uptree = uptree.getOwner(); 
                    if (uptree.getStorageQuota() != null) {
                        return uptree.getStorageQuota().getAllocation();
                    }
                }
            }
        }
        
        return null;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }    
}

    
