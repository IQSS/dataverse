/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class LinkDatasetCommand extends AbstractCommand<DatasetLinkingDataverse> {
    
    private final Dataset linkedDataset;
    private final Dataverse linkingDataverse;
    
    public LinkDatasetCommand(DataverseRequest aRequest, Dataverse dataverse, Dataset linkedDataset) {
        super(aRequest, dataverse);
        this.linkedDataset = linkedDataset;
        this.linkingDataverse = dataverse;
    }

    @Override
    public DatasetLinkingDataverse execute(CommandContext ctxt) throws CommandException {
        
        if (!linkedDataset.isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.link.not.published"), this);
        }       
        if (linkedDataset.getOwner().equals(linkingDataverse)) {           
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.link.not.to.owner"), this);
        }
        if (linkedDataset.getOwner().getOwners().contains(linkingDataverse)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.link.not.to.parent.dataverse"), this);
        }
       
        DatasetLinkingDataverse datasetLinkingDataverse = new DatasetLinkingDataverse();
        datasetLinkingDataverse.setDataset(linkedDataset);
        datasetLinkingDataverse.setLinkingDataverse(linkingDataverse);
        datasetLinkingDataverse.setLinkCreateTime(new Timestamp(new Date().getTime()));
        ctxt.dsLinking().save(datasetLinkingDataverse);
        ctxt.em().flush();
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(linkedDataset, doNormalSolrDocCleanUp);
        return datasetLinkingDataverse;
    }  
}
