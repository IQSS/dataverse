/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.Discover )
public class GetDatasetCommand extends AbstractCommand<Dataset>{
    private final Dataset ds;

    public GetDatasetCommand(User aUser, Dataset anAffectedDataset) {
        super(aUser, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        return ds;
    }
    
    
    
}
