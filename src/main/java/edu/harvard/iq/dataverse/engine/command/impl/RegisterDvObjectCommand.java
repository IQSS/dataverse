package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.AlternativePersistentIdentifier;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;

import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions({})
public class RegisterDvObjectCommand extends AbstractVoidCommand {

    private final DvObject target;
    private final  Boolean migrateHandle;

    public RegisterDvObjectCommand(DataverseRequest aRequest, DvObject target) {
        super(aRequest, target);
        this.target = target;
        this.migrateHandle = false;
    }
    
    public RegisterDvObjectCommand(DataverseRequest aRequest, DvObject target, Boolean migrateHandle) {
        super(aRequest, target);
        this.target = target;
        this.migrateHandle = migrateHandle;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        
        DvObjectContainer container = (target instanceof DvObjectContainer) ? (DvObjectContainer) target : target.getOwner();
        // Get the pidProvider that is configured to mint new IDs
        PidProvider pidProvider = ctxt.dvObjects().getEffectivePidGenerator(container);
        if(this.migrateHandle){
            //Only continue if you can successfully migrate the handle
            if (HandlePidProvider.HDL_PROTOCOL.equals(pidProvider.getProtocol()) || !processMigrateHandle(ctxt)) return;
        }
        
        try {
            //Test to see if identifier already present
            //if so, leave.
            if (target.getIdentifier() == null || target.getIdentifier().isEmpty()) {
                pidProvider.generatePid(target);
            }

            if (pidProvider.alreadyRegistered(target)) {
                return;
            }
            String doiRetString = pidProvider.createIdentifier(target);
            if (doiRetString != null && doiRetString.contains(target.getIdentifier())) {
                if (!pidProvider.registerWhenPublished()) {
                    // Should register ID before publicize() is called
                    // For example, DOIEZIdProvider tries to recreate the id if the identifier isn't registered before
                    // publicizeIdentifier is called
                    target.setIdentifierRegistered(true);
                    target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                }
                if (target.isReleased()) {
                    pidProvider.publicizeIdentifier(target);
                }
                if (pidProvider.registerWhenPublished() && target.isReleased()) {
                    target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    target.setIdentifierRegistered(true);
                }
                ctxt.em().merge(target);
                ctxt.em().flush();
                if (target.isInstanceofDataset() && target.isReleased() && !this.migrateHandle) {
                    Dataset dataset = (Dataset) target;
                    for (DataFile df : dataset.getFiles()) {
                        if (df.getIdentifier() == null || df.getIdentifier().isEmpty()) {
                            pidProvider.generatePid(df);
                        }
                        doiRetString = pidProvider.createIdentifier(df);
                        if (doiRetString != null && doiRetString.contains(df.getIdentifier())) {
                            if (!pidProvider.registerWhenPublished()) {
                                // Should register ID before publicize() is called
                                // For example, DOIEZIdProvider tries to recreate the id if the identifier isn't registered before
                                // publicizeIdentifier is called
                                df.setIdentifierRegistered(true);
                                df.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                            }
                            if (df.isReleased()) {
                                pidProvider.publicizeIdentifier(df);
                            }
                            if (pidProvider.registerWhenPublished() && df.isReleased()) {
                                df.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                                df.setIdentifierRegistered(true);
                            }
                            ctxt.em().merge(df);
                            ctxt.em().flush();
                        }
                    }
                }

            } else {
                //do nothing - we'll know it failed because the global id create time won't have been updated.
            }
        } catch (Exception e) {
            //do nothing - idem and the problem has been logged
        } catch (Throwable ex) {
            //do nothing - we'll know it failed because the global id create time won't have been updated.
        }
        if (this.migrateHandle) {
            //Only continue if you can successfully migrate the handle
            boolean doNormalSolrDocCleanUp = true;
            Dataset dataset = (Dataset) target;
            ctxt.index().asyncIndexDataset(dataset, doNormalSolrDocCleanUp);
            ctxt.solrIndex().indexPermissionsForOneDvObject( dataset);
        }
    }
    
    private Boolean processMigrateHandle (CommandContext ctxt){
        boolean retval = true;
        if(!target.isInstanceofDataset()) return false;
        if(!target.getProtocol().equals(HandlePidProvider.HDL_PROTOCOL)) return false;
        
        AlternativePersistentIdentifier api = new AlternativePersistentIdentifier();
        api.setProtocol(target.getProtocol());
        api.setAuthority(target.getAuthority());
        api.setIdentifier(target.getIdentifier());
        api.setDvObject(target);
        api.setIdentifierRegistered(target.isIdentifierRegistered());
        api.setGlobalIdCreateTime(target.getGlobalIdCreateTime());
        api.setStorageLocationDesignator(true);
        ctxt.em().persist(api);
        target.setProtocol(null);
        target.setAuthority(null);
        target.setIdentifier(null);
        target.setIdentifierRegistered(false);
        target.setGlobalIdCreateTime(null);
        return retval;
    }
        
}
