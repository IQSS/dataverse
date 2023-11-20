/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.storageuse.StorageQuota;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class DeleteCollectionQuotaCommand  extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(DeleteCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse targetDataverse;
    
    public DeleteCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        targetDataverse = target;
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        if (targetDataverse == null) {
            throw new IllegalCommandException("", this);
        }
        
        StorageQuota storageQuota = targetDataverse.getStorageQuota();
        
        if (storageQuota != null && storageQuota.getAllocation() != null) {
            storageQuota.setAllocation(null);
            ctxt.em().merge(storageQuota);
            ctxt.em().flush();
        } 
        // ... and if no quota was enabled on the collection - nothing to do = success
    }    
}