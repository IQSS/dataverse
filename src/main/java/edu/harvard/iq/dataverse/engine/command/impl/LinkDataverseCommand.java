/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataverse)
public class LinkDataverseCommand extends AbstractCommand<DataverseLinkingDataverse> {
    
    private final Dataverse linkedDataverse;
    private final Dataverse linkingDataverse;
    
    public LinkDataverseCommand(DataverseRequest aRequest, Dataverse dataverse, Dataverse linkedDataverse) {
        super(aRequest, dataverse);
        this.linkedDataverse = linkedDataverse;
        this.linkingDataverse = dataverse;
    }

    @Override
    public DataverseLinkingDataverse execute(CommandContext ctxt) throws CommandException {
        DataverseLinkingDataverse dataverseLinkingDataverse = new DataverseLinkingDataverse();
        dataverseLinkingDataverse.setDataverse(linkedDataverse);
        dataverseLinkingDataverse.setLinkingDataverse(linkingDataverse);
        dataverseLinkingDataverse.setLinkCreateTime(new Timestamp(new Date().getTime()));
        ctxt.dvLinking().save(dataverseLinkingDataverse);
        ctxt.index().indexDataverse(linkedDataverse);
        return dataverseLinkingDataverse;
    }   
}
