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
import java.util.logging.Logger;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "", value = Permission.UndoableEdit),
    @RequiredPermissions(dataverseName = "", value = Permission.EditMetadata)
})
public class UpdateDatasetCommand extends AbstractCommand<Dataset> {
   private static final Logger logger = Logger.getLogger(UpdateDatasetCommand.class.getCanonicalName());
    private final Dataset theDataset;

    public UpdateDatasetCommand(Dataset theDataset, DataverseUser user) {
        super(user, theDataset);
        this.theDataset = theDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        return save(ctxt);
    }

    public void saveDatasetAPI(CommandContext ctxt) {
        save(ctxt);
    }


    public Dataset save(CommandContext ctxt) {
        Iterator<DatasetField> dsfIt = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }        
        Iterator<DatasetField> dsfItSort = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
           dsfItSort.next().setValueDisplayOrder();
        }
        String indexingResult = ctxt.index().indexDataset(theDataset);
        logger.info("during dataset save, indexing result was: " + indexingResult);
        Dataset savedDataset = ctxt.em().merge(theDataset);
        return savedDataset;
    }

    

}
