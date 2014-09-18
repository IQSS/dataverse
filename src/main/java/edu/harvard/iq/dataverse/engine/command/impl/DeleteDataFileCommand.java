package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccessObject;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;

/**
 * Deletes a data file, both DB entity and filesystem object.
 *
 * @author michael
 */
@RequiredPermissions(Permission.DestructiveEdit)
public class DeleteDataFileCommand extends AbstractVoidCommand {
    private static final Logger logger = Logger.getLogger(DeleteDataFileCommand.class.getCanonicalName());

    private final DataFile doomed;

    public DeleteDataFileCommand(DataFile doomed, DataverseUser aUser) {
        super(aUser, doomed);
        this.doomed = doomed;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (doomed.isReleased()) {
            logger.fine("Delete command called on a released (published) DataFile "+doomed.getId());
            /*
             If the file has been released but also previously published handle here.
             In this case we're only removing the link to the current version
             we're not deleting the underlying data file
             */
            if (ctxt.files().isPreviouslyPublished(doomed.getId())) {
                //if previously published leave physical file alone for prior versions
                FileMetadata fmr = doomed.getFileMetadatas().get(0);
                for (FileMetadata testfmd : doomed.getFileMetadatas()) {
                    if (testfmd.getDatasetVersion().getId() > fmr.getDatasetVersion().getId()) {
                        fmr = testfmd;
                    }
                }
                FileMetadata doomedAndMerged = ctxt.em().merge(fmr);
                ctxt.em().remove(doomedAndMerged);
                String indexingResult = ctxt.index().removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierFile + doomed.getId() + "_draft");
                return;
            }
            throw new IllegalCommandException("Cannot delete a released file", this);
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
