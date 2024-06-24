package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
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
@RequiredPermissions(Permission.ManageDataversePermissions)
// alternatively, we could make it dynamic - public for published collections
// and Permission.ViewUnpublishedDataverse required otherwise (?)
public class GetCollectionStorageUseCommand extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetCollectionStorageUseCommand.class.getCanonicalName());
    
    private final Dataverse collection;
    
    public GetCollectionStorageUseCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        collection = target;
    } 
        
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
               
        if (collection == null) {
            throw new CommandException("null collection passed to get storage use command", this);
        }
        return ctxt.storageUse().findStorageSizeByDvContainerId(collection.getId());        
    }

    /*@Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }*/   
}