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
import edu.harvard.iq.dataverse.util.StringUtil;
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
 * but with some extra twists. The goal is to avoid creating new Dataset and 
 * DataFile objects, and to instead preserve the database ids of the re-harvested 
 * datasets and files, whenever possible. This in turn allows us to avoid deleting
 * and rebuilding from scratch the Solr documents for these objects. 
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
        
        newHarvestedVersion.setCreateTime(getTimestamp());
        newHarvestedVersion.setLastUpdateTime(getTimestamp());

        
        Map<String, Integer> existingFilesIndex = new HashMap<>();

        /* 
        Create a map of the files that are currently part of this existing
        harvested dataset. We assume that a harvested file can be uniquely 
        defined by its storageidentifier. Which, in the case of a datafile
        harvested from another Dataverse should be its data access api url.
        */
        for (int i = 0; i < existingDataset.getFiles().size(); i++) {
            String storageIdentifier = existingDataset.getFiles().get(i).getStorageIdentifier();
            if (!StringUtil.isEmpty(storageIdentifier)) {
                existingFilesIndex.put(storageIdentifier, i);
            }
        }
        
        /*
        Go through the files in the newly-harvested version and check if any of 
        them are (potentially new/updated) versions of files that we already
        have, harvested previously from the same archive location. 
        */
        for (FileMetadata newFileMetadata : newHarvestedVersion.getFileMetadatas()) {
            // is it safe to assume that each new FileMetadata will be 
            // pointing to a non-null DataFile here?
            String storageIdentifier = newFileMetadata.getDataFile().getStorageIdentifier();
            if (!StringUtil.isEmpty(storageIdentifier) && existingFilesIndex.containsKey(storageIdentifier)) {
                newFileMetadata.getDataFile().setFileMetadatas(new ArrayList<>());

                int fileIndex = existingFilesIndex.get(storageIdentifier);
                
                // Make sure to update the existing DataFiles that we are going 
                // to keep in case their newly-harvested versions have different 
                // checksums, mime types etc. These values are supposed to be 
                // immutable, normally - but who knows, errors happen, the source 
                // Dataverse may have had to fix these in their database to 
                // correct a data integrity issue (for ex.):
                existingDataset.getFiles().get(fileIndex).setContentType(newFileMetadata.getDataFile().getContentType());
                existingDataset.getFiles().get(fileIndex).setFilesize(newFileMetadata.getDataFile().getFilesize());
                existingDataset.getFiles().get(fileIndex).setChecksumType(newFileMetadata.getDataFile().getChecksumType());
                existingDataset.getFiles().get(fileIndex).setChecksumValue(newFileMetadata.getDataFile().getChecksumValue());
                
                // Point the newly-harvested filemetadata to the existing datafile:
                newFileMetadata.setDataFile(existingDataset.getFiles().get(fileIndex));
                
                // Make sure this new FileMetadata is the only one attached to this existing file:
                existingDataset.getFiles().get(fileIndex).setFileMetadatas(new ArrayList<>(1));
                existingDataset.getFiles().get(fileIndex).getFileMetadatas().add(newFileMetadata);
                // (we don't want any cascade relationships left between this existing
                // dataset and this version, since we are going to attemp to delete it).
                
                // Drop the file from the index map:
                existingFilesIndex.remove(storageIdentifier);
            }
        }
        
        // @todo? check if there's anything special that needs to be done for things
        // like file categories
                
        List<String> solrIdsOfDocumentsToDelete = new ArrayList<>();

        // Go through the existing files and delete the ones that are 
        // no longer present in the version that we have just harvesed:
        for (FileMetadata oldFileMetadata : existingDataset.getVersions().get(0).getFileMetadatas()) {
            DataFile oldDataFile = oldFileMetadata.getDataFile();
            String storageIdentifier = oldDataFile.getStorageIdentifier();
            // Is it still in the existing files map? - that means it is no longer
            // present in the newly-harvested version
            if (StringUtil.isEmpty(storageIdentifier) || existingFilesIndex.containsKey(storageIdentifier)) {
                solrIdsOfDocumentsToDelete.add(solrDocIdentifierFile + oldDataFile.getId());
                existingDataset.getFiles().remove(oldDataFile);
                // Files from harvested datasets are removed unceremoniously, 
                // directly in the database. No need to bother calling the 
                // DeleteFileCommand on them. We'll just need to remember to purge
                // them from Solr as well (right below)
                ctxt.em().remove(ctxt.em().merge(oldDataFile));
                // (no need to explicitly remove the oldFileMetadata; it will be 
                // removed with the entire old version is deleted)
            }
        }
                
        // purge all the SOLR documents associated with the files
        // we have just deleted:
        if (!solrIdsOfDocumentsToDelete.isEmpty()) {
            ctxt.index().deleteHarvestedDocuments(solrIdsOfDocumentsToDelete);
        }

        // ... And now delete the existing version itself: 
        existingDataset.setVersions(new ArrayList<>());
        existingVersion.setDataset(null);
        
        existingVersion = ctxt.em().merge(existingVersion);
        ctxt.em().remove(existingVersion);

        // Now attach the newly-harvested version to the dataset:
        existingDataset.getVersions().add(newHarvestedVersion);
        newHarvestedVersion.setDataset(existingDataset);
                
        // ... There's one more thing to do - go through the new files, 
        // that are not in the database yet, and make sure they are 
        // attached to this existing dataset, instead of the dummy temp 
        // dataset into which they were originally imported:
        for (FileMetadata newFileMetadata : newHarvestedVersion.getFileMetadatas()) {
            if (newFileMetadata.getDataFile().getId() == null) {
                existingDataset.getFiles().add(newFileMetadata.getDataFile());
                newFileMetadata.getDataFile().setOwner(existingDataset);
            }
        }
                
        ctxt.em().persist(newHarvestedVersion);
                
        Dataset savedDataset = ctxt.em().merge(existingDataset);
        ctxt.em().flush();
                
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
