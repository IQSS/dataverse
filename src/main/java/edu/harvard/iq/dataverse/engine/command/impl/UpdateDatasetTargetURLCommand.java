/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetTargetURLCommand extends AbstractVoidCommand  {

    private final Dataset target;
    private final User user;
    
    public UpdateDatasetTargetURLCommand( Dataset target, User user) {
        super(user, target);
        this.target = target;
        this.user = user;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        
        HashMap<String, String> metadata = new HashMap<String, String>();
        System.out.print(user);
        if ( !(user instanceof AuthenticatedUser) || !((AuthenticatedUser) user).isSuperuser()  ) {      
            throw new PermissionException("Update Target URL can only be called by superusers.",
                this,  Collections.singleton(Permission.EditDataset), target);                
        }
        
        if (target.getProtocol().equals("doi")){
            metadata = ctxt.doiEZId().getMetadataFromDatasetForTargetURL(target);
            String doiRetString = ctxt.doiEZId().modifyIdentifier(target, metadata);
            if (doiRetString.contains(target.getIdentifier())) {
                target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                ctxt.em().merge(target);
                ctxt.em().flush();
            } else {
                //do nothing - we'll know it failed because the global id create time won't have been updated.
            }
        } else if ("hdl".equals(target.getProtocol())) {
            // TODO: 
            // handlenet registration still needs diagnostics! 
            // -- L.A. 4.0
            ctxt.handleNet().reRegisterHandle(target);
            target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
            ctxt.em().merge(target);
            ctxt.em().flush();
        } else {
            throw new UnsupportedOperationException("UpdateDatasetTargetURLCommand only supported for doi protocol."); //To change body of generated methods, choose Tools | Templates.  
        }
                          
    }
    
}
