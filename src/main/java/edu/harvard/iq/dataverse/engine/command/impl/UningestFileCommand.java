/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import jakarta.persistence.Query;

/**
 *
 * @author skraffmi
 * @author Leonid Andreev 
 */
@RequiredPermissions({})
public class UningestFileCommand extends AbstractVoidCommand  {

    private static final Logger logger = Logger.getLogger(UningestFileCommand.class.getCanonicalName());
    final DataFile uningest;
    
    public UningestFileCommand(DataverseRequest aRequest, DataFile uningest) {
        super(aRequest, uningest);
        this.uningest = uningest;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        
        // first check if  user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException("Uningest File can only be called by Superusers.",
                this,  Collections.singleton(Permission.EditDataset), uningest);                
        }
        
        // is this actually a tabular data file?
        if (!uningest.isTabularData()) {
            throw new IllegalCommandException("UningestFileCommand called on a non-tabular data file (id="+uningest.getId()+")", this);
        }

        String originalFileName = uningest.getOriginalFileName();

        StorageIO<DataFile> dataAccess = null;
        // size of the stored original:
        Long storedOriginalFileSize;

        // Try to open the main storageIO for the file; look up the AUX file for 
        // the saved original and check its size:
        try {
            dataAccess = DataAccess.getStorageIO(uningest);
            dataAccess.open();
            storedOriginalFileSize = dataAccess.getAuxObjectSize(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioex) {
            String errorMessage = "Failed to open StorageIO for " + uningest.getStorageIdentifier() + " attempting to revert tabular ingest" +  " aborting. (";
            if (ioex.getMessage() != null) {
                errorMessage += "(" + ioex.getMessage() + ")";
            } else {
                errorMessage += "(IOException caught; no further information is available)";
            }
            logger.warning(errorMessage);
            throw new CommandException(errorMessage, this);
            
        } 
          
        // Try to revert the backup-as-Aux:
        // (i.e., try to overwrite the current, tabular file, with the stored 
        // original file:
        // (if this fails, we definitely want to abort the whole thing and bail!)
        // -- L.A. May 2018
        
        try {
            dataAccess.revertBackupAsAux(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioex) {
            String errorMessage = "Failed to revert backup as Aux for " + uningest.getStorageIdentifier() + " attempting to revert tabular ingest" +  " aborting. (";
            if (ioex.getMessage() != null) {
                errorMessage += "(" + ioex.getMessage() + ")";
            } else {
                errorMessage += "(IOException caught; no further information is available)";
            }
            logger.warning(errorMessage);
            throw new CommandException(errorMessage, this);
        } 
        
        // OK, we have successfully reverted the backup - now let's change 
        // all the attribute of the file that are stored in the database: 
        
        // the file size: 
        uningest.setFilesize(storedOriginalFileSize);
        
        // original file format:
        String originalFileFormat = uningest.getDataTable().getOriginalFileFormat();
        uningest.setContentType(originalFileFormat);
        
        
        // Delete the DataTable database object hierarchy that stores
        // all the tabular metadata - (DataTable, DataVariable, SummaryStatistics
        // *and more* sub-objects:
        
        //removeSummaryStatistics(uningest, ctxt);
        DataTable dataTable = ctxt.em().find(DataTable.class, uningest.getDataTable().getId());
        ctxt.em().remove(dataTable);
        uningest.setDataTable(null);

        // remove the IngestReport associated with this datafile: 
        // (this is a single table entry; ok to just issue an explicit 
        // DELETE query for it - as there's no complex cascade to resolve)
        resetIngestStats(uningest, ctxt);
        
        //probably unnecessary - why would you add tags to a file and then say "oops this shouldn't have been ingested"?
        DataFileTag tag;
        for (DataFileTag tagLoop: uningest.getTags()){
            tag = ctxt.em().find(DataFileTag.class, tagLoop.getId());
            ctxt.em().remove(tag);
        }
        uningest.setTags(null);        
        // Do the DB merge:
        ctxt.em().merge(uningest); 
        
        // Modify the file name - which is stored in FileMetadata, and there
        // could be more than one: 
        
       // String originalExtension = FileUtil.generateOriginalExtension(originalFileFormat);
        for (FileMetadata fm : uningest.getFileMetadatas()) {
            
            fm.setLabel(originalFileName);
            ctxt.em().merge(fm);
            
            /* 
            getOriginalFileName method replaces this code
            String filename = fm.getLabel();
            String extensionToRemove = StringUtil.substringIncludingLast(filename, ".");
            if (StringUtil.nonEmpty(extensionToRemove)) {
                String newFileName = filename.replace(extensionToRemove, originalExtension);
                fm.setLabel(newFileName);
                ctxt.em().merge(fm);
            }
             */
            
            DatasetVersion dv = fm.getDatasetVersion();
            
            // And, while we are here, recalculate the UNF for this DatasetVersion:
            dv.setUNF(null);
            ctxt.em().merge(dv);
            ctxt.datasetVersion().fixMissingUnf(dv.getId().toString(), true);
        }

        try{
            dataAccess.deleteAllAuxObjects();
        } catch (IOException e){
            logger.warning("Io Exception deleting all aux objects : " + uningest.getId());
        }
        
    }
    
    
    private void resetIngestStats(DataFile uningest, CommandContext ctxt){
        
        Long fileid = uningest.getId();        
        Query query = ctxt.em().createQuery("DELETE from IngestReport as o where o.dataFile.id  =:fileid");
        query.setParameter("fileid", fileid);
        query.executeUpdate();       
        uningest.setIngestStatus("A".charAt(0));
        
    }  

}
