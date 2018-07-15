package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetVersionCommand.class.getCanonicalName());
    private final List<FileMetadata> filesToDelete;
    private boolean validateLenient = false;
    
    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
        this.filesToDelete = new ArrayList<>();
    }    
    
    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, List<FileMetadata> filesToDelete) {
        super(aRequest, theDataset);
        this.filesToDelete = filesToDelete;
    }
    
    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, DataFile fileToDelete) {
        super(aRequest, theDataset);
        
        // get the latest file metadata for the file; ensuring that it is a draft version
        this.filesToDelete = new ArrayList<>();
        for (FileMetadata fmd : theDataset.getEditVersion().getFileMetadatas()) {
            if (fmd.getDataFile().equals(fileToDelete)) {
                filesToDelete.add(fmd);
                break;
            }
        }
    }    

    public boolean isValidateLenient() {
        return validateLenient;
    }

    public void setValidateLenient(boolean validateLenient) {
        this.validateLenient = validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        if ( ! (getUser() instanceof AuthenticatedUser) ) {
            throw new IllegalCommandException("Only authenticated users can update datasets", this);
        }
        
        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks prventing the update
        
        getDataset().getEditVersion().setDatasetFields(getDataset().getEditVersion().initDatasetFields());
        validateOrDie( getDataset().getEditVersion(), isValidateLenient() );
        
        final DatasetVersion editVersion = getDataset().getEditVersion();
        tidyUpFields(editVersion);
        
        // Merge the new version into out JPA context, if needed.
        if ( editVersion.getId() == null || editVersion.getId() == 0L ) {
            ctxt.em().persist(editVersion);
        } else {
            ctxt.em().merge(editVersion);
        }
        
        for (DataFile dataFile : getDataset().getFiles()) {
            if (dataFile.getCreateDate() == null) {
                dataFile.setCreateDate(getTimestamp());
                dataFile.setCreator((AuthenticatedUser) getUser());
            }
            dataFile.setModificationTime(getTimestamp());
        }
                
        // Remove / delete any files that were removed
        
        // If any of the files that we are deleting has a UNF, we will need to 
        // re-calculate the UNF of the version - since that is the product 
        // of the UNFs of the individual files. 
        boolean recalculateUNF = false;
        /* The separate loop is just to make sure that the dataset database is 
        updated, specifically when an image datafile is being deleted, which
        is being used as the dataset thumbnail as part of a batch delete. 
        if we don't remove the thumbnail association with the dataset before the 
        actual deletion of the file, it might throw foreign key integration 
        violation exceptions. 
        */
        for (FileMetadata fmd : filesToDelete){
             //  check if this file is being used as the default thumbnail
            if (fmd.getDataFile().equals(getDataset().getThumbnailFile())) {
                logger.fine("deleting the dataset thumbnail designation");
                getDataset().setThumbnailFile(null);
            }
            
            if (fmd.getDataFile().getUnf() != null) {
                recalculateUNF = true;
            }
        }
        // we have to merge to update the database but not flush because 
        // we don't want to create two draft versions!
        Dataset tempDataset = ctxt.em().merge(getDataset());
        
        for (FileMetadata fmd : filesToDelete) {
            if (!fmd.getDataFile().isReleased()) {
                // if file is draft (ie. new to this version, delete; otherwise just remove filemetadata object)
                ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getRequest()));
                tempDataset.getFiles().remove(fmd.getDataFile());
                tempDataset.getEditVersion().getFileMetadatas().remove(fmd);
                // added this check to handle issue where you could not deleter a file that shared a category with a new file
                // the relation ship does not seem to cascade, yet somehow it was trying to merge the filemetadata
                // todo: clean this up some when we clean the create / update dataset methods
                for (DataFileCategory cat : tempDataset.getCategories()) {
                    cat.getFileMetadatas().remove(fmd);
                }
            } else {
                FileMetadata mergedFmd = ctxt.em().merge(fmd);
                ctxt.em().remove(mergedFmd);
                fmd.getDataFile().getFileMetadatas().remove(fmd);
                tempDataset.getEditVersion().getFileMetadatas().remove(fmd);
            }      
        }        
        
        if (recalculateUNF) {
            ctxt.ingest().recalculateDatasetVersionUNF(tempDataset.getEditVersion());
        }
        
        tempDataset.getEditVersion().setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());
         
        Dataset savedDataset = ctxt.em().merge(tempDataset);
        ctxt.em().flush();

        updateDatasetUser(ctxt);
        ctxt.index().indexDataset(savedDataset, true);

        return savedDataset; 
    }

}
