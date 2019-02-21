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

/**
 *
 * @author matthew
 */
@RequiredPermissions({})
public class ChangeUserIdentifierCommand extends AbstractVoidCommand {

    final AuthenticatedUser au;
    final BuiltinUser bu;
    final String newIdentifier;
    final List<RoleAssignment> raList;
    
    public ChangeUserIdentifierCommand(DataverseRequest aRequest, AuthenticatedUser au, BuiltinUser bu, String newIdentifier, List<RoleAssignment> raList) {
        super(
                aRequest,
                (DvObject) null
        );
        this.au = au;
        this.newIdentifier = newIdentifier;
        this.bu = bu;
        this.raList = raList;
    }
    
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {     
        au.setUserIdentifier(newIdentifier);
        bu.setUserName(newIdentifier);
        
        //Validate the BuiltinUser change. Username validations are there.
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
        
        AuthenticatedUserLookup aul = au.getAuthenticatedUserLookup();
        aul.setPersistentUserId(newIdentifier);
        
        for(RoleAssignment ra : raList) {
            if(ra.getAssigneeIdentifier().charAt(0) == '@') {
                ra.setAssigneeIdentifier("@" + newIdentifier);
            } else {
                throw new IllegalCommandException("Original userIdentifier provided does not seem to be an AuthenticatedUser", this);
            }
        }
    }
}
