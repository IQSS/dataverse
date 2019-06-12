/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class GetDataverseStorageSizeCommand extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetDataverseStorageSizeCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    
    public GetDataverseStorageSizeCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        dataverse = target;
    } 
    
    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
        logger.fine("getDataverseStorageSize called on "+dataverse.getAlias());
       
        
        long total = 0L; 
        List<Long> childDatasets = ctxt.dataverses().findAllDataverseDatasetChildren(dataverse.getId());
        
        for (Long childId : childDatasets) {
            Dataset dataset = ctxt.datasets().find(childId);
            
            if (dataset == null) {
                // should never happen - must indicate some data corruption in the database
                throw new CommandException(BundleUtil.getStringFromBundle("dataverse.listing.error"), this); 
            }
            
            try {
                total += ctxt.datasets().findStorageSize(dataset);
            } catch (IOException ex) {
                throw new CommandException(BundleUtil.getStringFromBundle("dataverse.totalsize.ioerror"), this);
            }
        }
        
        return total;
    }    
}