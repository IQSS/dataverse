/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.Response;

/**
 *
 * @author matthew
 */
@RequiredPermissions({})
public class ChangeUserIdentifierCommand extends AbstractVoidCommand {

    final AuthenticatedUser au;
    final String newIdentifier;
    final String oldIdentifier;
    
    public ChangeUserIdentifierCommand(DataverseRequest aRequest, AuthenticatedUser au, String newIdentifier) {
        super(
            aRequest,
            (DvObject) null
        );
        this.au = au;
        this.newIdentifier = newIdentifier;
        this.oldIdentifier = au.getUserIdentifier();
    }
    
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {  
        
        AuthenticatedUser authenticatedUserTestNewIdentifier = ctxt.authentication().getAuthenticatedUser(newIdentifier);
        if (authenticatedUserTestNewIdentifier != null) {
            String logMsg = " User " + newIdentifier + " already exists. Cannot use this as new identifier";
            throw new IllegalCommandException("Validation of submitted data failed. Details: " + logMsg, this);
        }
        
        List<RoleAssignment> raList = ctxt.roleAssignees().getAssignmentsFor(au.getIdentifier()); //only AuthenticatedUser supported
        BuiltinUser bu = ctxt.builtinUsers().findByUserName(oldIdentifier);
        au.setUserIdentifier(newIdentifier);
        
        if (bu != null) {
            bu.setUserName(newIdentifier);
            //Validate the BuiltinUser change. Username validations are there.
            //If we have our validation errors pass up to commands, this could be removed
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<BuiltinUser>> violations = validator.validate(bu);
            int numViolations = violations.size();
            if (numViolations > 0) {
                StringBuilder logMsg = new StringBuilder();
                for (ConstraintViolation<?> violation : violations) {
                    logMsg.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
                }
                throw new IllegalCommandException("Validation of submitted data failed. Details: " + logMsg, this);
            }
        }
        
        ctxt.actionLog().changeUserIdentifierInHistory("@" + oldIdentifier, "@" + newIdentifier);
        
        AuthenticatedUserLookup aul = au.getAuthenticatedUserLookup();
        aul.setPersistentUserId(newIdentifier);
        
        for(RoleAssignment ra : raList) {
            ra.setAssigneeIdentifier("@" + newIdentifier);
        }
    }
    
    @Override
    public String describe() {
        return "User " + oldIdentifier + " renamed to " + newIdentifier;
    }
}
