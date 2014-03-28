/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.UndoableEdit)
public class UpdateDatasetCommand extends AbstractCommand<Dataset> {

    	private final Dataset theDataset;

	public UpdateDatasetCommand(Dataset theDataset, DataverseUser user) {
		super( user, theDataset.getOwner() );
		this.theDataset = theDataset;
	}
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        return ctxt.datasets().save(theDataset);
       
    }
    
}
