/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Collections;

/**
 * @author sarahferry
 */

@RequiredPermissions(Permission.EditDataset)
public class DeleteDatasetLinkingDataverseCommand extends AbstractCommand<Dataset> {
    private final DatasetLinkingDataverse doomed;
    private final Dataset editedDs;
    private final boolean index;

    public DeleteDatasetLinkingDataverseCommand(DataverseRequest aRequest, Dataset editedDs, DatasetLinkingDataverse doomed, boolean index) {
        super(aRequest, editedDs);
        this.editedDs = editedDs;
        this.doomed = doomed;
        this.index = index;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException("Delete dataset linking dataverse can only be called by superusers.",
                                          this, Collections.singleton(Permission.EditDataset), editedDs);
        }
        Dataset merged = ctxt.em().merge(editedDs);
        DatasetLinkingDataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);

        if (index) {
            ctxt.index().indexDataset(editedDs, true);
            ctxt.index().indexDataverse(doomed.getLinkingDataverse());
        }
        return merged;
    }
}
