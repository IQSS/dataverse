package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
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
 * @author michael
 */
@RequiredPermissions( Permission.AddDataset )
public class CreateDatasetVersionCommand extends AbstractDatasetCommand<DatasetVersion> {
    
    private static final Logger logger = Logger.getLogger(CreateDatasetVersionCommand.class.getName());
    
    final DatasetVersion newVersion;
    final Dataset dataset;
    
    public CreateDatasetVersionCommand(DataverseRequest aRequest, Dataset theDataset, DatasetVersion aVersion) {
        super(aRequest, theDataset);
        dataset = theDataset;
        newVersion = aVersion;
    }
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        DatasetVersion latest = dataset.getLatestVersion();
        if ( latest.isWorkingCopy() ) {
            // A dataset can only have a single draft, which has to be the latest.
            // This is imposed here.
            if (newVersion.getVersionState().equals(VersionState.DRAFT)){
                throw new IllegalCommandException("Latest version is already a draft. Cannot add another draft", this);
            }
        }
        
        prepareDatasetAndVersion();
        
        List<FileMetadata> newVersionMetadatum = new ArrayList<>(latest.getFileMetadatas().size());
        for ( FileMetadata fmd : latest.getFileMetadatas() ) {
            FileMetadata fmdCopy = fmd.createCopy();
            fmdCopy.setDatasetVersion(newVersion);
            newVersionMetadatum.add( fmdCopy );
        }
        newVersion.setFileMetadatas(newVersionMetadatum);
        
        // TODO make async
        // ctxt.index().indexDataset(dataset);
        return ctxt.datasets().storeVersion(newVersion);
        
    }
    
    /**
     * Updates the states of the dataset and the new dataset version, such that
     * the new version becomes the latest version of the dataset. Also initializes
     * the internal fields of the dataset version.
     * 
     * @throws CommandException 
     */
    public void prepareDatasetAndVersion() throws CommandException {
        newVersion.setDataset(dataset);
        newVersion.setDatasetFields(newVersion.initDatasetFields());
        newVersion.setCreateTime(getTimestamp());
        newVersion.setLastUpdateTime(getTimestamp());
        
        tidyUpFields(newVersion);
        validateOrDie(newVersion, false);
        
        final List<DatasetVersion> currentVersions = dataset.getVersions();
        ArrayList<DatasetVersion> dsvs = new ArrayList<>(currentVersions.size());
        dsvs.addAll(currentVersions);
        dsvs.add(0, newVersion);
        dataset.setVersions( dsvs );
        dataset.setModificationTime(getTimestamp());
    }
    
}
