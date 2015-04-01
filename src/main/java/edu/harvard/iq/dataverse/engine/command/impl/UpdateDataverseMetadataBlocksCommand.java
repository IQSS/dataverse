package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Commands for setting the metadata blocks a dataverse uses.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataverse )
public abstract class UpdateDataverseMetadataBlocksCommand extends AbstractVoidCommand {
    
    final Dataverse updatedDv;
    
    public UpdateDataverseMetadataBlocksCommand(User aUser, Dataverse anAffectedDataverse) {
        super(aUser, anAffectedDataverse);
        updatedDv = anAffectedDataverse;
    }
    
    
    public static class SetRoot extends UpdateDataverseMetadataBlocksCommand {    
        
        private final boolean isRoot;
            
        public SetRoot(User aUser, Dataverse anAffectedDataverse, boolean shouldBeRoot) {
            super(aUser, anAffectedDataverse);
            isRoot = shouldBeRoot;
        }

        @Override
        protected void executeImpl(CommandContext ctxt) throws CommandException {
            updatedDv.setMetadataBlockRoot(isRoot);
            ctxt.em().merge(updatedDv);
        }
        
    }
    
    public static class SetBlocks extends UpdateDataverseMetadataBlocksCommand {    
        
        private final List<MetadataBlock> blocks;
            
        public SetBlocks(User aUser, Dataverse anAffectedDataverse, List<MetadataBlock> someBlocks) {
            super(aUser, anAffectedDataverse);
            blocks = someBlocks;
        }

        @Override
        protected void executeImpl(CommandContext ctxt) throws CommandException {
            ctxt.engine().submit( new UpdateDataverseMetadataBlocksCommand.SetRoot(getUser(), updatedDv, true) );
            
            // We filter the list through a set, so that all blocks are distinct.
            updatedDv.setMetadataBlocks(new LinkedList<>(new HashSet<>(blocks)));
            ctxt.em().merge(updatedDv);
        }
    }
}
