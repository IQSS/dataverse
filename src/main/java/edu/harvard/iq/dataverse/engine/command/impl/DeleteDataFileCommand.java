package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataFileIO;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * Deletes a data file, both DB entity and filesystem object.
 *
 * @author michael
 */
@RequiredPermissions(Permission.EditDataset)
public class DeleteDataFileCommand extends AbstractVoidCommand {
    private static final Logger logger = Logger.getLogger(DeleteDataFileCommand.class.getCanonicalName());

    private final DataFile doomed;
    private final boolean destroy;

    public DeleteDataFileCommand(DataFile doomed, DataverseRequest aRequest) {
        this(doomed, aRequest, false);
    }
    
    public DeleteDataFileCommand(DataFile doomed, DataverseRequest aRequest, boolean destroy) {
        super(aRequest, doomed.getOwner());
        this.doomed = doomed;
        this.destroy = destroy;
    }    

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (destroy) {
            //todo: clean this logic up!
            //for now, if called as destroy, will check for superuser acess
            if ( doomed.getOwner().isReleased() && (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
                throw new PermissionException("Destroy can only be called by superusers.",
                    this,  Collections.singleton(Permission.DeleteDatasetDraft), doomed);                
            }            
        } else {
            // since this is not a destroy, we want to make sure the file is a draft
            // we'll do three sanity checks
            // 1. confirm the file is not released
            // 2. confirm the file is only attached to one version (i.e. only has one fileMetadata)
            // 3. confirm that version is not released
            if (doomed.isReleased() || doomed.getFileMetadatas().size() > 1 || doomed.getFileMetadata().getDatasetVersion().isReleased()) {
                throw new CommandException("Cannot delete file: the DataFile is published, is attached to more than one Dataset Version, or is attached to a released Dataset Version.", this);  
            }            
        }
        

        
        // We need to delete a bunch of files from the file system;
        // First we try to delete the data file itself; if that 
        // fails, we throw an exception and abort the command without
        // trying to remove the object from the database:
        
        
        // TODO: !!
        // The code below assumes all the files are stored locally, on the filesystem!
        // -- L.A. 4.0.2
        logger.log(Level.FINE, "Delete command called on an unpublished DataFile {0}", doomed.getId());
        String fileSystemName = doomed.getStorageIdentifier();
        logger.log(Level.FINE, "Storage identifier for the file: {0}", fileSystemName);
        
        DataFileIO dataAccess = null; 
        
        try {
            dataAccess = doomed.getAccessObject();
        } catch (IOException ioex) {
            throw new CommandExecutionException("Failed to initialize physical access driver.", ioex, this);
        }
        
        if (dataAccess != null) {
            // If this is a local file, we only want to attempt to delete it 
            // if it actually exists on the filesystem: 
            // TODO: 
            // add a generic .exists() method to the dataAccess object. 
            // -- L.A. 4.0
            boolean physicalFileExists = false;
            
            if (dataAccess.isLocalFile()) {
                try {
                    if (dataAccess.getFileSystemPath() != null
                            && dataAccess.getFileSystemPath().toFile() != null
                            && dataAccess.getFileSystemPath().toFile().exists()) {
                        physicalFileExists = true;
                    }
                } catch (IOException ioex) {
                    physicalFileExists = true;
                }
            }

            if (physicalFileExists || (!dataAccess.isLocalFile())) {
                try {
                    dataAccess.delete();
                } catch (IOException ex) {
                    throw new CommandExecutionException("Error deleting physical file object while deleting DataFile " + doomed.getId() + " from the database.", ex, this);
                }
            }
            
            logger.log(Level.FINE, "Successfully deleted physical storage object (file) for the DataFile {0}", doomed.getId());
            
            // Destroy the dataAccess object - we will need to purge the 
            // DataFile from the database (below), so we don't want to have any
            // objects in this transaction that reference it:
            
            dataAccess = null; 
            
            // We may also have a few extra files associated with this object - 
            // preserved original that was used in the tabular data ingest, 
            // cached R data frames, image thumbnails, etc.
            // We need to delete these too; failures however are less 
            // important with these. If we fail to delete any of these 
            // auxiliary files, we'll just leave an error message in the 
            // log file and proceed deleting the database object.
            
            // Note that the assumption here is that all these auxiliary 
            // files - saved original, cached format conversions, etc., are
            // all stored on the physical filesystem locally. 
            // TODO: revisit and review this assumption! -- L.A. 4.0
            
            List<Path> victims = new ArrayList<>();

            // 1. preserved original: 
            Path filePath = doomed.getSavedOriginalFile();
            if (filePath != null) {
                victims.add(filePath);
            }

            // 2. Cached files: 
            victims.addAll(listCachedFiles(doomed));

            // Delete them all: 
            List<String> failures = new ArrayList<>();
            for (Path deadFile : victims) {
                try {
                    logger.log(Level.FINE, "Deleting cached file {0}", deadFile.toString());
                    Files.delete(deadFile);
                } catch (IOException ex) {
                    failures.add(deadFile.toString());
                }
            }

            if (!failures.isEmpty()) {
                String failedFiles = StringUtils.join(failures, ",");
                Logger.getLogger(DeleteDataFileCommand.class.getName()).log(Level.SEVERE, "Error deleting physical file(s) {0} while deleting DataFile {1}", new Object[]{failedFiles, doomed.getName()});
            }
                    
            DataFile doomedAndMerged = ctxt.em().merge(doomed);
            ctxt.em().remove(doomedAndMerged);
            /**
             * @todo consider adding an em.flush here (despite the performance
             * impact) if you need to operate on the dataset below. Without the
             * flush, the dataset still thinks it has the file that was just
             * deleted.
             */
            // ctxt.em().flush();
            
            /**
             * We *could* re-index the entire dataset but it's more efficient to
             * target individual files for deletion, which should always be
             * drafts.
             *
             * See also https://redmine.hmdc.harvard.edu/issues/3786
             */
            String indexingResult = ctxt.index().removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierFile + doomed.getId() + "_draft");
            /**
             * @todo check indexing result for success or failure. Really, we
             * need an indexing queuing system:
             * https://redmine.hmdc.harvard.edu/issues/3643
             */
        }

    }

    private List<Path> listCachedFiles(DataFile dataFile) {
        List<Path> victims = new ArrayList<>();

        // cached files for a given datafiles are stored on the filesystem
        // as <filesystemname>.*; for example, <filename>.thumb64 or 
        // <filename>.RData.
        final String baseName = dataFile.getStorageIdentifier();

        if (baseName == null || baseName.equals("")) {
            return null;
        }

        Path datasetDirectory = dataFile.getOwner().getFileSystemDirectory();

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path file) throws IOException {
                return (file.getFileName() != null
                        && file.getFileName().toString().startsWith(baseName + "."));
            }
        };

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(datasetDirectory, filter)) {
            for (Path filePath : dirStream) {
                victims.add(filePath);
            }
        } catch (IOException ex) {
        }

        return victims;
    }

}
