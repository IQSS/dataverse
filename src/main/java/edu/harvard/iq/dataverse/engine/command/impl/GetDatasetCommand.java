/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.Access )
public class GetDatasetCommand extends AbstractCommand<Dataset>{
    private final Dataset ds;

    public GetDatasetCommand(DataverseUser aUser, Dataset anAffectedDataset) {
        super(aUser, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        return ds;
    }
    
    
    
}
