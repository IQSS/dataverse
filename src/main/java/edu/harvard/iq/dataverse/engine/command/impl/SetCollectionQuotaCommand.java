/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.storageuse.StorageQuota;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 *
 * A superuser-only command:
 */
@RequiredPermissions({})
public class SetCollectionQuotaCommand  extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(GetCollectionQuotaCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    private final Long allocation; 
    
    public SetCollectionQuotaCommand(DataverseRequest aRequest, Dataverse target, Long allocation) {
        super(aRequest, target);
        dataverse = target;
        this.allocation = allocation; 
    } 
        
    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // Check if user is a superuser:
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException(BundleUtil.getStringFromBundle("dataverse.storage.quota.superusersonly"),
                this,  null, dataverse);                
        }
        
        if (dataverse == null) {
            throw new IllegalCommandException("Must specify valid collection", this);
        }
        
        if (allocation == null) {
            throw new IllegalCommandException("Must specify valid allocation in bytes", this);
        }
        
        StorageQuota storageQuota = dataverse.getStorageQuota();
        
        if (storageQuota != null) {
            storageQuota.setAllocation(allocation);
            ctxt.em().merge(storageQuota);
        } else {
            storageQuota = new StorageQuota(); 
            storageQuota.setDefinitionPoint(dataverse);
            storageQuota.setAllocation(allocation);
            dataverse.setStorageQuota(storageQuota);
            ctxt.em().persist(storageQuota);
        }
        ctxt.em().flush();
    }    
}
