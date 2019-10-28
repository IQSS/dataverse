package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Commands for setting the metadata blocks a dataverse uses.
 *
 * @author michael
 */
@RequiredPermissions(Permission.EditDataverse)
public abstract class UpdateDataverseMetadataBlocksCommand extends AbstractVoidCommand {

    final Dataverse updatedDv;

    public UpdateDataverseMetadataBlocksCommand(DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        updatedDv = anAffectedDataverse;
    }


    public static class SetRoot extends UpdateDataverseMetadataBlocksCommand {

        private final boolean isRoot;

        public SetRoot(DataverseRequest aRequest, Dataverse anAffectedDataverse, boolean shouldBeRoot) {
            super(aRequest, anAffectedDataverse);
            isRoot = shouldBeRoot;
        }

        @Override
        protected void executeImpl(CommandContext ctxt) {
            updatedDv.setMetadataBlockRoot(isRoot);
            ctxt.em().merge(updatedDv);
        }

    }

    public static class SetBlocks extends UpdateDataverseMetadataBlocksCommand {

        private final List<MetadataBlock> blocks;

        public SetBlocks(DataverseRequest aRequest, Dataverse anAffectedDataverse, List<MetadataBlock> someBlocks) {
            super(aRequest, anAffectedDataverse);
            blocks = someBlocks;
        }

        @Override
        protected void executeImpl(CommandContext ctxt)  {
            ctxt.engine().submit(new UpdateDataverseMetadataBlocksCommand.SetRoot(getRequest(), updatedDv, true));

            // We filter the list through a set, so that all blocks are distinct.
            updatedDv.setMetadataBlocks(new LinkedList<>(new HashSet<>(blocks)));
            ctxt.em().merge(updatedDv);
        }
    }
}
