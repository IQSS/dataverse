/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.inject.Named;

/**
 *
 * @author skraffmi
 */
@Stateless
@Named
public class EjbDataverseEngineInner {
    
    @TransactionAttribute(REQUIRED)
    public <R> R submit(Command<R> aCommand, CommandContext ctxt) throws CommandException {
        
        return aCommand.execute(ctxt);
    
    }
    
}
