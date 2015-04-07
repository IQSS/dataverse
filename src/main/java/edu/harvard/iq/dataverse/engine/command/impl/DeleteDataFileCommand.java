package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccessObject;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
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
    private final User user;
    private final boolean destroy;

    public DeleteDataFileCommand(DataFile doomed, User aUser) {
        this(doomed, aUser, false);
    }
    
    public DeleteDataFileCommand(DataFile doomed, User aUser, boolean destroy) {
        super(aUser, doomed.getOwner());
        this.doomed = doomed;
        this.user = aUser;
        this.destroy = destroy;
    }    

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (destroy) {
            //todo: clean this logic up!
            //for now, if called as destroy, will check for superuser acess
            if ( doomed.getOwner().isReleased() && (!(user instanceof AuthenticatedUser) || !((AuthenticatedUser) user).isSuperuser() ) ) {      
                throw new PermissionException("Destroy can only be called by superusers.",
                    this,  Collections.singleton(Permission.DeleteDatasetDraft), doomed);                
            }            
        }
        
        
        // if destroy, we skip this and fully delete
        if (doomed.isReleased() && !destroy) {
            logger.fine("Delete command called on a released (published) DataFile "+doomed.getId());
            /*
             In this case we're only removing the link to the current version
             we're not deleting the underlying data file
             */    
            DatasetVersion dsv = doomed.getOwner().getEditVersion();
            for (FileMetadata fmd : dsv.getFileMetadatas()) {
                if (doomed.getId() != null && doomed.equals(fmd.getDataFile())) {
                    // first create draft, if it's new
                    if (dsv.getId() == null) {
                        ctxt.engine().submit(new UpdateDatasetCommand(dsv.getDataset(), user));
                    }
                    
                    FileMetadata doomedAndMerged = ctxt.em().merge(fmd);
                    ctxt.em().remove(doomedAndMerged);
                    String indexingResult = ctxt.index().removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierFile + doomed.getId() + "_draft");
                    return;
                }                    
            } 
            throw new CommandException("Could not find the file to be deleted in the draft version of the dataset", this);
        }

        // We need to delete a bunch of files from the file system;
        // First we try to delete the data file itself; if that 
        // fails, we throw an exception and abort the command without
        // trying to remove the object from the database:
        
        logger.fine("Delete command called on an unpublished DataFile "+doomed.getId());
        String fileSystemName = doomed.getFileSystemName();
        logger.fine("Storage identifier for the file: "+fileSystemName);
        
        DataAccessObject dataAccess = null; 
        
        try {
            dataAccess = doomed.getAccessObject();
        } catch (IOException ioex) {
            throw new CommandExecutionException("Failed to initialize physical access driver.", ioex, this);
        }
        
        if (dataAccess != null) {
            
            try {
                dataAccess.delete();
            } catch (IOException ex) {
                throw new CommandExecutionException("Error deleting physical file object while deleting DataFile " + doomed.getId() + " from the database.", ex, this);
            }

            logger.fine("Successfully deleted physical storage object (file) for the DataFile " + doomed.getId());
            
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
                    logger.fine("Deleting cached file "+deadFile.toString());
                    Files.delete(deadFile);
                } catch (IOException ex) {
                    failures.add(deadFile.toString());
                }
            }

            if (!failures.isEmpty()) {
                String failedFiles = StringUtils.join(failures, ",");
                Logger.getLogger(DeleteDataFileCommand.class.getName()).log(Level.SEVERE, "Error deleting physical file(s) " + failedFiles + " while deleting DataFile " + doomed.getName());
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
        final String baseName = dataFile.getFileSystemName();

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
