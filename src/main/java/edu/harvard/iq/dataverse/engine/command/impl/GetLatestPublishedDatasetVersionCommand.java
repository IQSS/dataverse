package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * @author Naomi
 */
// No permission needed to view published dvObjects
@RequiredPermissions({})
public class GetLatestPublishedDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    private final Dataset ds;
    private final boolean includeDeaccessioned;
    private boolean checkPerms;

    public GetLatestPublishedDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset) {
        this(aRequest, anAffectedDataset, false, false);
    }

    public GetLatestPublishedDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset, boolean includeDeaccessioned, boolean checkPerms) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
        this.includeDeaccessioned = includeDeaccessioned;
        this.checkPerms = checkPerms;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {

        for (DatasetVersion dsv : ds.getVersions()) {
            if (dsv.isReleased() || (includeDeaccessioned && dsv.isDeaccessioned())) {
                
                if(dsv.isDeaccessioned() && checkPerms){
                    if(!ctxt.permissions().requestOn(getRequest(), ds).has(Permission.EditDataset)){
                        return null;
                    }
                }
                return dsv;
            }
        }
        return null;
    }
}
