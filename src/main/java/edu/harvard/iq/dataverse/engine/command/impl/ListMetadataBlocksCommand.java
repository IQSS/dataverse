package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the metadata blocks of a {@link Dataverse}.
 * 
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class ListMetadataBlocksCommand extends AbstractCommand<List<MetadataBlock>>{
    
    private final Dataverse dv;
    
    public ListMetadataBlocksCommand(DataverseRequest aRequest, Dataverse aDataverse) {
        super(aRequest, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) throws CommandException {
        return dv.getMetadataBlocks();
    }
    
    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }    
    
}
