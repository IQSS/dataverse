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
    private boolean checkPermsWhenDeaccessioned;

    public GetLatestPublishedDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset) {
        this(aRequest, anAffectedDataset, false, false);
    }

    public GetLatestPublishedDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset, boolean includeDeaccessioned, boolean checkPermsWhenDeaccessioned) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
        this.includeDeaccessioned = includeDeaccessioned;
        this.checkPermsWhenDeaccessioned = checkPermsWhenDeaccessioned;
    }

    /*
    * This command depending on the requested parameters will return:
    * 
    * If the user requested to include a deaccessioned dataset with the files, the command will return the deaccessioned version if the user has permissions to view the files. Otherwise, it will return null. 
    * If the user requested to include a deaccessioned dataset but did not request the files, the command will return the deaccessioned version. 
    * If the user did not request to include a deaccessioned dataset, the command will return the latest published version.
    *  
    */
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {

        DatasetVersion dsv = null;

        //We search of a released or deaccessioned version if it is requested.
        for (DatasetVersion next : ds.getVersions()) {
            if (next.isReleased() || (includeDeaccessioned && next.isDeaccessioned())){
                dsv = next;
                break;
            }
        }

        //Checking permissions if the deaccessionedVersion was found and we are checking permissions because files were requested.
        if(dsv != null && (dsv.isDeaccessioned() && checkPermsWhenDeaccessioned)){
            //If the user has no permissions we return null
            if(!ctxt.permissions().requestOn(getRequest(), ds).has(Permission.EditDataset)){
                dsv = null;
            }
        }

        return dsv;
    }



}
