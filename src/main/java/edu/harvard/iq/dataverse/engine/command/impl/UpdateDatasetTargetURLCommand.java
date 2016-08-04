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
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetTargetURLCommand extends AbstractVoidCommand  {

    private final Dataset target;
    
    public UpdateDatasetTargetURLCommand( Dataset target, DataverseRequest aRequest) {
        super(aRequest, target);
        this.target = target;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Update Target URL can only be called by superusers.",
                    this, Collections.singleton(Permission.EditDataset), target);
        }

        if (target.getProtocol().equals("doi")) {
            String nonNullDefaultIfKeyNotFound = "";
            String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
            if (doiProvider.equals("EZID")) {
                HashMap<String, String> metadata = ctxt.doiEZId().getMetadataFromDatasetForTargetURL(target);
                String doiRetString = ctxt.doiEZId().modifyIdentifier(target, metadata);
                if (doiRetString != null && doiRetString.contains(target.getIdentifier())) {
                    target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    ctxt.em().merge(target);
                    ctxt.em().flush();
                } else {
                    //do nothing - we'll know it failed because the global id create time won't have been updated.
                }
            }
            if (doiProvider.equals("DataCite")) {
                HashMap<String, String> metadata = ctxt.doiDataCite().getMetadataFromDatasetForTargetURL(target);
                try {
                    String doiRetString = ctxt.doiDataCite().modifyIdentifier(target, metadata);
                    if (doiRetString != null && doiRetString.contains(target.getIdentifier())) {
                        target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                        ctxt.em().merge(target);
                        ctxt.em().flush();
                    } else {
                        //do nothing - we'll know it failed because the global id create time won't have been updated.
                    }
                } catch (Exception e) {
                    //do nothing - we'll know it failed because the global id create time won't have been updated.
                }
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
            // TODO why not throw an IllegalCommandException?
            throw new UnsupportedOperationException("UpdateDatasetTargetURLCommand only supported for doi protocol."); //To change body of generated methods, choose Tools | Templates.  
        }
                          
    }
    
}
