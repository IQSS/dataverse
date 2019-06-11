/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.sql.Timestamp;
import java.util.Collections;
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
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException("Link Dataverse can only be called by superusers.",
                    this, Collections.singleton(Permission.PublishDataverse), linkingDataverse);
        }
        if (linkedDataverse.equals(linkingDataverse)) {
            throw new IllegalCommandException("Can't link a dataverse to itself", this);
        }
        if (linkedDataverse.getOwners().contains(linkingDataverse)) {
            throw new IllegalCommandException("Can't link a dataverse to its parents", this);
        }
        
        DataverseLinkingDataverse dataverseLinkingDataverse = new DataverseLinkingDataverse();
        dataverseLinkingDataverse.setDataverse(linkedDataverse);
        dataverseLinkingDataverse.setLinkingDataverse(linkingDataverse);
        dataverseLinkingDataverse.setLinkCreateTime(new Timestamp(new Date().getTime()));
        ctxt.dvLinking().save(dataverseLinkingDataverse);
        ctxt.index().indexDataverse(linkedDataverse);
        return dataverseLinkingDataverse;
    }   
}
