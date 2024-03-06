/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
 * Get the latest version of a dataset a user can view.
 *
 * @author Naomi
 */
// No permission needed to view published dvObjects
@RequiredPermissions({})
public class GetLatestAccessibleDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    private final Dataset ds;
    private final boolean includeDeaccessioned;
    private boolean checkPerms;

    public GetLatestAccessibleDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset) {
        this(aRequest, anAffectedDataset, false, false);
    }

    public GetLatestAccessibleDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset, boolean includeDeaccessioned, boolean checkPerms) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
        this.includeDeaccessioned = includeDeaccessioned;
        this.checkPerms = checkPerms;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        if (ds.getLatestVersion().isDraft() && ctxt.permissions().requestOn(getRequest(), ds).has(Permission.ViewUnpublishedDataset)) {
            return ctxt.engine().submit(new GetDraftDatasetVersionCommand(getRequest(), ds));
        }
        return ctxt.engine().submit(new GetLatestPublishedDatasetVersionCommand(getRequest(), ds, includeDeaccessioned, checkPerms));
    }
}
