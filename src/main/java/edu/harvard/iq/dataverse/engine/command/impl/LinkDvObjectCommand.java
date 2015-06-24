/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.LinkedDvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions(Permission.PublishDataverse)
public class LinkDvObjectCommand extends AbstractCommand<LinkedDvObject>{
    private final DvObject linkedDvObject;
    private final Dataverse linkingDvObject;
    
   public LinkDvObjectCommand(User aUser, Dataverse linkingDvObject, DvObject linkedDvObject) {
        super(aUser, linkingDvObject);
        this.linkedDvObject = linkedDvObject;
        this.linkingDvObject = linkingDvObject;
    }

    @Override
    public LinkedDvObject execute(CommandContext ctxt) throws CommandException {
        LinkedDvObject linkedDvObjectRet = new LinkedDvObject();
        linkedDvObjectRet.setDvObject(linkedDvObject);
        linkedDvObjectRet.setOwner(linkingDvObject);
        linkedDvObjectRet.setLinkCreateTime(new Timestamp(new Date().getTime()));
        ctxt.dvoLinking().save(linkedDvObjectRet);
        
        if (linkedDvObject instanceof Dataverse ){
           ctxt.index().indexDataverse((Dataverse) linkedDvObject); 
        }
        if (linkedDvObject instanceof Dataset ){
           boolean doNormalSolrDocCleanUp = true;
           ctxt.index().indexDataset((Dataset) linkedDvObject, doNormalSolrDocCleanUp); 
        }
            
        return linkedDvObjectRet;
    }   
    
}
