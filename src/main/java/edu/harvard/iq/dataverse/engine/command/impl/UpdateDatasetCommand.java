/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Iterator;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "", value = Permission.UndoableEdit),
    @RequiredPermissions(dataverseName = "", value = Permission.EditMetadata)
})
public class UpdateDatasetCommand extends AbstractCommand<Dataset> {

    private final Dataset theDataset;

    public UpdateDatasetCommand(Dataset theDataset, DataverseUser user) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        return save(ctxt);
    }

    public void saveDatasetAPI(CommandContext ctxt) {
        save(ctxt);
    }

    public Dataset release(CommandContext ctxt) {
        Dataset savedDataset = ctxt.em().merge(theDataset);
        //String indexingResult = indexService.indexDataset(savedDataset);
        return savedDataset;
    }

    public Dataset save(CommandContext ctxt) {

        Iterator<DatasetField> dsfIt = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (ctxt.datasets().removeBlankDatasetFieldValues(dsfIt.next())) {
                dsfIt.remove();
            }
        }

        Dataset savedDataset = ctxt.em().merge(theDataset);

        return savedDataset;
    }

    

}
