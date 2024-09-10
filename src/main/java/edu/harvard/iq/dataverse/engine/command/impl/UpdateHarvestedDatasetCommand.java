package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import static edu.harvard.iq.dataverse.search.IndexServiceBean.solrDocIdentifierFile;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author landreev
 * 
 * Much simplified version of UpdateDatasetVersionCommand, 
 * but with some extra twists. 
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateHarvestedDatasetCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateHarvestedDatasetCommand.class.getCanonicalName());
    private final DatasetVersion newHarvestedVersion;
    final private boolean validateLenient = true;

    public UpdateHarvestedDatasetCommand(Dataset theDataset, DatasetVersion newHarvestedVersion, DataverseRequest aRequest) {
        super(aRequest, theDataset);
        this.newHarvestedVersion = newHarvestedVersion;
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        
        // ... do the magic - parse the version json, do the switcheroo ...
        Dataset existingDataset = getDataset();

        if (existingDataset == null
                || existingDataset.getId() == null
                || !existingDataset.isHarvested()
                || existingDataset.getVersions().size() != 1) {
            throw new IllegalCommandException("The command can only be called on an existing harvested dataset with only 1 version", this);
        }
        DatasetVersion existingVersion = existingDataset.getVersions().get(0);

        if (newHarvestedVersion == null || newHarvestedVersion.getId() != null) {
            throw new IllegalCommandException("The command can only be called with a newly-harvested, not yet saved DatasetVersion supplied", this);
        }
        
        Map<String, Integer> existingFilesIndex = new HashMap<>();

        for (int i = 0; i < existingDataset.getFiles().size(); i++) {
            String storageIdentifier = existingDataset.getFiles().get(i).getStorageIdentifier();
            if (storageIdentifier != null) {
                existingFilesIndex.put(storageIdentifier, i);
            }
        }
                
        for (FileMetadata newFileMetadata : newHarvestedVersion.getFileMetadatas()) {
            // is it safe to assume that each new FileMetadata will be 
            // pointing to a non-null DataFile here?
            String location = newFileMetadata.getDataFile().getStorageIdentifier();
            if (location != null && existingFilesIndex.containsKey(location)) {
                newFileMetadata.getDataFile().setFileMetadatas(new ArrayList<>());

                int fileIndex = existingFilesIndex.get(location);
                newFileMetadata.setDataFile(existingDataset.getFiles().get(fileIndex));
                existingDataset.getFiles().get(fileIndex).getFileMetadatas().add(newFileMetadata);
                existingFilesIndex.remove(location);
            }
        }
        // @todo check that the newly-harvested DataFiles have the same checksums
        // and mime types etc. These values are supposed to be immutable, normally, 
        // but who knows - they may have fixed something invalid on the other end
        // @todo check if there's anything special that needs to be done for things
        // like file categories
                
        List<String> solrIdsOfDocumentsToDelete = new ArrayList<>();

        // Go through the existing files and delete the ones that are 
        // no longer present in the version that we have just harvesed:
        for (FileMetadata oldFileMetadata : existingDataset.getVersions().get(0).getFileMetadatas()) {
            DataFile oldDataFile = oldFileMetadata.getDataFile();
            solrIdsOfDocumentsToDelete.add(solrDocIdentifierFile + oldDataFile.getId());
            existingDataset.getFiles().remove(oldDataFile);
            // Files from harvested datasets are removed unceremoniously, 
            // directly in the database. No need to bother calling the 
            // DeleteFileCommand on them.
            ctxt.em().remove(ctxt.em().merge(oldDataFile));
            ctxt.em().remove(ctxt.em().merge(oldFileMetadata));
            oldDataFile = null;
            oldFileMetadata = null;
        }
                
        // purge all the SOLR documents associated with the files
        // we have just deleted:
        if (!solrIdsOfDocumentsToDelete.isEmpty()) {
            ctxt.index().deleteHarvestedDocuments(solrIdsOfDocumentsToDelete);
        }

        // ... And now delete the existing version itself: 
        existingDataset.setVersions(new ArrayList<>());
        ctxt.em().remove(ctxt.em().merge(existingVersion));

        // Now attach the newly-harvested version to the dataset:
        existingDataset.getVersions().add(newHarvestedVersion);
        newHarvestedVersion.setDataset(existingDataset);
                
        // ... There's one more thing to do - go through the new files, 
        // that are not in the database yet, and make sure they are 
        // attached to this existing dataset:
        for (FileMetadata newFileMetadata : newHarvestedVersion.getFileMetadatas()) {
            if (newFileMetadata.getDataFile().getId() == null) {
                existingDataset.getFiles().add(newFileMetadata.getDataFile());
                newFileMetadata.getDataFile().setOwner(existingDataset);
            }
        }
                
        ctxt.em().persist(newHarvestedVersion);
                
        Dataset savedDataset = ctxt.em().merge(existingDataset);
        ctxt.em().flush();
        
        //@todo reindex 
        
        return savedDataset;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        Dataset d = (Dataset) r;
        
        try {
            // Note that we index harvested datasets synchronously:
            ctxt.index().indexDataset(d, true);
        } catch (SolrServerException|IOException solrServerEx) {
            logger.log(Level.WARNING, "Exception while trying to index the updated Harvested dataset " + d.getGlobalId().asString(), solrServerEx.getMessage());
            retVal = false;
        }
        
        return retVal;
    }
}
