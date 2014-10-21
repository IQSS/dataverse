package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.List;

/**
 * Lists the metadata blocks of a {@link Dataverse}.
 * 
 * @author michael
 */
@RequiredPermissions( Permission.Discover )
public class ListMetadataBlocksCommand extends AbstractCommand<List<MetadataBlock>>{
    
    private final Dataverse dv;
    
    public ListMetadataBlocksCommand(User aUser, Dataverse aDataverse) {
        super(aUser, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) throws CommandException {
        return dv.getMetadataBlocks();
    }
    
}
