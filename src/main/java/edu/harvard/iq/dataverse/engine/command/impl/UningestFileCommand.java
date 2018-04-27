/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
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
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions({})
public class UningestFileCommand extends AbstractVoidCommand  {

    private static final Logger logger = Logger.getLogger(MoveDatasetCommand.class.getCanonicalName());
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
        
        String fileName;

            StorageIO<DataFile> dataAccess = null;
            Long originalFileSize;

            try{
                dataAccess = DataAccess.getStorageIO(uningest);
                dataAccess.open();
                originalFileSize = dataAccess.getSize();
                fileName = dataAccess.getFileName();
                uningest.setFilesize(originalFileSize);
                dataAccess.revertBackupAsAux(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
            }
        
        
            catch (IOException ioex) {
            logger.warning("Failed to find file for uningest " + uningest.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
        } 
        
        String originalFileFormat = getOriginalFileFormat(uningest, ctxt);
        String originalExtension = FileUtil.generateOriginalExtension(originalFileFormat);
        removeSummaryStatistics(uningest, ctxt);
        resetIngestStats(uningest, ctxt);
        uningest.setContentType(originalFileFormat);
        uningest.setDataTable(null);
        ctxt.em().merge(uningest); 
        for (FileMetadata fm : uningest.getFileMetadatas()) {
            String filename = fm.getLabel();
            String extensionToRemove = StringUtil.substringIncludingLast(filename, ".");
            String newFileName = filename.replace(extensionToRemove, originalExtension);
            fm.setLabel(newFileName);
            DatasetVersion dv = fm.getDatasetVersion();
            dv.setUNF(null);
            ctxt.em().merge(dv);
            ctxt.datasetVersion().fixMissingUnf(dv.getId().toString(), true);
        }
    }

    
    private String getOriginalFileFormat(DataFile uningest, CommandContext ctxt) {
        Long datatableid = uningest.getDataTable().getId();
        String originalFileFormat = "";
        Query query = ctxt.em().createQuery("select o.originalFileFormat from DataTable as o where o.id  =:datatableid");
        query.setParameter("datatableid", datatableid);
        originalFileFormat = (String) query.getSingleResult();
        return originalFileFormat;
    }
    
    
    private void removeSummaryStatistics(DataFile uningest, CommandContext ctxt){
        Long datatableid = uningest.getDataTable().getId();;        
        Query  query = ctxt.em().createQuery("DELETE from SummaryStatistic as o where o.dataVariable.id in (Select dv.id from DataVariable as dv where dv.dataTable.id =:datatableid)");
        query.setParameter("datatableid", datatableid);
        query.executeUpdate();
        query = ctxt.em().createQuery("DELETE from DataVariable as o where o.dataTable.id  =:datatableid");
        query.setParameter("datatableid", datatableid);
        query.executeUpdate();
        query = ctxt.em().createQuery("DELETE from DataTable as o where o.id  =:datatableid");
        query.setParameter("datatableid", datatableid);
        query.executeUpdate();
    }
    
    
    private void resetIngestStats(DataFile uningest, CommandContext ctxt){
        
        Long fileid = uningest.getId();        
        Query query = ctxt.em().createQuery("DELETE from IngestReport as o where o.dataFile.id  =:fileid");
        query.setParameter("fileid", fileid);
        query.executeUpdate();       
        uningest.setIngestStatus("A".charAt(0));
        
    }  

}
