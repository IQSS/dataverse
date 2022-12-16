/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.logging.Logger;



/**
 *
 * Restrict or unrestricts an existing datafile
 * 
 * @author sarahferry
 */

@RequiredPermissions(Permission.EditDataset)
public class RestrictFileCommand extends AbstractVoidCommand {
    private static final Logger logger = Logger.getLogger(RestrictFileCommand.class.getCanonicalName());

    private final DataFile file;
    private final boolean restrict;
    
    public RestrictFileCommand(DataFile file, DataverseRequest aRequest, boolean restrict) {
        super(aRequest, file.getOwner());
        this.file = file;
        this.restrict = restrict;
    }    
       
    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // check if public install & don't allow
        boolean defaultValue = false;
        boolean publicInstall = ctxt.settings().isTrueForKey(SettingsServiceBean.Key.PublicInstall, defaultValue);
        
        if (publicInstall) {
            throw new CommandExecutionException("Restricting files is not permitted on a public installation.", this);
        }
        // check if this file is already restricted or already unrestricted
        if (restrict == file.getFileMetadata().isRestricted()) {
            String text = restrict ? "restricted" : "unrestricted";
            throw new CommandExecutionException("File " + file.getDisplayName() + " is already " + text, this);
        }
        // At present 4.9.4, it doesn't appear that new files use this command, so owner should always be set...
        if (file.getOwner() == null) {
            // this is a new file through upload, restrict
            file.getFileMetadata().setRestricted(restrict);
            file.setRestricted(restrict);
        }
        else {
            Dataset dataset = file.getOwner();
            DatasetVersion workingVersion = dataset.getOrCreateEditVersion();
            // We need the FileMetadata for the file in the draft dataset version and the
            // file we have may still reference the fmd from the prior released version
            FileMetadata draftFmd = file.getFileMetadata();
            if (dataset.isReleased()) {
                // We want to update the draft version, which may not exist (if the file has
                // been deleted from an existing draft, so we want null unless this file's
                // metadata can be found in the current version
                draftFmd=null;
                for (FileMetadata fmw : workingVersion.getFileMetadatas()) {
                    if (file.equals(fmw.getDataFile())) {
                        draftFmd = fmw;
                        break;
                    }
                }
            }
            if (draftFmd != null) {
                draftFmd.setRestricted(restrict);
                if (!file.isReleased()) {
                    file.setRestricted(restrict);
                }
            }
        }
    }
}
