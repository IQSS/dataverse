/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
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
public class GetSpecificPublishedDatasetVersionCommand extends AbstractCommand<DatasetVersion>{
    private final Dataset ds;
    private final long majorVersion;
    private final long minorVersion;
    
    public GetSpecificPublishedDatasetVersionCommand(User aUser, Dataset anAffectedDataset, long majorVersionNum, long minorVersionNum) {
        super(aUser, anAffectedDataset);
        ds = anAffectedDataset;
        majorVersion = majorVersionNum;
        minorVersion = minorVersionNum;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        for (DatasetVersion dsv: ds.getVersions()) {
            if (dsv.isReleased()) {
                if (dsv.getVersionNumber().equals(majorVersion) && dsv.getMinorVersionNumber().equals(minorVersion)) {
                    return dsv;
                }
            }
        }
        return null;
    }
    
}