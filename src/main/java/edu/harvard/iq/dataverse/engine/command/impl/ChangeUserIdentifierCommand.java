/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import javax.ejb.EJB;

/**
 *
 * @author matthew
 */
@RequiredPermissions({})
public class ChangeUserIdentifierCommand extends AbstractVoidCommand {

    final AuthenticatedUser au;
    final BuiltinUser bu;
    final String newIdentifier;
    
    public ChangeUserIdentifierCommand(DataverseRequest aRequest, AuthenticatedUser au, BuiltinUser bu, String newIdentifier) {
        super(
                aRequest,
                (DvObject) null
        );
        this.au = au;
        this.newIdentifier = newIdentifier;
        this.bu = bu;
    }
    
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {

        

        
//        if (bu == null || aul == null) {
//            throw new Exception("BLAH");
//        }
        
        au.setUserIdentifier(newIdentifier);
        bu.setUserName(newIdentifier);
        AuthenticatedUserLookup aul = au.getAuthenticatedUserLookup();
        aul.setPersistentUserId(newIdentifier);
        
        
        //roleassignment - has an @ attached to the front
        
        //Need to do roles?
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
