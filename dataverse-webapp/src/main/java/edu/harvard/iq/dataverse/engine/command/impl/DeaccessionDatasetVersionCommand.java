/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class DeaccessionDatasetVersionCommand extends AbstractCommand<DatasetVersion> {

    final DatasetVersion theVersion;

    public DeaccessionDatasetVersionCommand(DataverseRequest aRequest, DatasetVersion deaccessionVersion) {
        super(aRequest, deaccessionVersion.getDataset());
        theVersion = deaccessionVersion;
    }


    @Override
    public DatasetVersion execute(CommandContext ctxt)  {

        theVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        DatasetVersion managed = ctxt.em().merge(theVersion);

        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(managed.getDataset(), doNormalSolrDocCleanUp);

        ctxt.em().merge(managed.getDataset());

        return managed;
    }

}
