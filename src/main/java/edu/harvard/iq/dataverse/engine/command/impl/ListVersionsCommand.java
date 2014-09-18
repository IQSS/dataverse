/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.Access )
public class ListVersionsCommand extends AbstractCommand<List<DatasetVersion>>{
    
    private final Dataset ds;
    
	public ListVersionsCommand(DataverseUser aUser, Dataset aDataset) {
		super(aUser, aDataset);
		ds = aDataset;
	}

	@Override
	public List<DatasetVersion> execute(CommandContext ctxt) throws CommandException {
		List<DatasetVersion> outputList = new LinkedList<>();
		for ( DatasetVersion dsv : ds.getVersions() ) {
            if (dsv.isReleased() || ctxt.permissions().on(ds).user(getUser()).has(Permission.AddDatasetVersion)) {
                outputList.add(dsv);
            }
		}
        return outputList;
	}
}
