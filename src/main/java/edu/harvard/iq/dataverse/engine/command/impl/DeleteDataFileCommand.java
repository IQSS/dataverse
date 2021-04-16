package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import java.sql.Timestamp;
import java.util.Date;

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
            if (doomed.getOwner().isReleased() && (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
                throw new PermissionException("Destroy can only be called by superusers.",
                        this, Collections.singleton(Permission.DeleteDatasetDraft), doomed);
            }
        } else // since this is not a destroy, we want to make sure the file is a draft
        // we'll do three sanity checks
        // 1. confirm the file is not released
        // 2. confirm the file is only attached to one version (i.e. only has one fileMetadata)
        // 3. confirm that version is not released
        if (doomed.isReleased() || doomed.getFileMetadatas().size() > 1 || doomed.getFileMetadata().getDatasetVersion().isReleased()) {
            throw new CommandException("Cannot delete file: the DataFile is published, is attached to more than one Dataset Version, or is attached to a released Dataset Version.", this);
        }

        // We need to delete a bunch of physical files, either from the file system,
        // or from some other storage medium where the datafile is stored, 
        // via its StorageIO driver.
        // First we delete the derivative files, then try to delete the data 
        // file itself; if that 
        // fails, we throw an exception and abort the command without
        // trying to remove the object from the database.
        // However, we do not attempt to do any deletes if this is a Harvested
        // file. 
        logger.log(Level.FINE, "Delete command called on an unpublished DataFile {0}", doomed.getId());

        if (!doomed.isHarvested() && !StringUtil.isEmpty(doomed.getStorageIdentifier())) {
            
            // "Package" files need to be treated as a special case, because, 
            // as of now, they are not supported by StorageIO (they only work 
            // with local filesystem as the storage mechanism). 
            
            if (FileUtil.isPackageFile(doomed)) {
                try { 
                    String datasetDirectory = doomed.getOwner().getFileSystemDirectory().toString();
                    Path datasetDirectoryPath = Paths.get(datasetDirectory, doomed.getStorageIdentifier());
                            
                        Files.walkFileTree(datasetDirectoryPath, new SimpleFileVisitor<Path>(){
                        @Override 
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                          throws IOException {
                          Files.delete(file);
                          return FileVisitResult.CONTINUE;
                        }

                        @Override 
                        public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                          return handleException(e);
                        }

                        private FileVisitResult handleException(final IOException e) {
                          logger.warning("Failed to delete file due to"+e.getMessage());
                          return FileVisitResult.TERMINATE;
                        }

                        @Override 
                        public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                          throws IOException {
                          if(e!=null)return handleException(e);
                          Files.delete(dir);
                          return FileVisitResult.CONTINUE;
                        }
                        }
                );
                    
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to delete package file "+doomed.getStorageIdentifier(), ioex, this);
                }
                
                logger.info("Successfully deleted the package file "+doomed.getStorageIdentifier());
                
            } else {
                logger.fine("Skipping deleting the physical file on the storage volume (will be done outside the command)");
                /* We no longer attempt to delete the physical file from inside the command, 
                 * since commands are executed as (potentially nested) transactions, 
                 * and it is prudent to assume that this database transaction may 
                 * be reversed in the end. Meaning if we delete the file here, 
                 * we are at risk of the database entry not getting deleted, 
                 * leaving a "ghost" DataFile with no associated physical file
                 * on the storage medium. 
                 * The physical file delete must happen outside the transaction, 
                 * once the database delete has been confirmed. 
                 */
                /*
                logger.log(Level.FINE, "Storage identifier for the file: {0}", doomed.getStorageIdentifier());
                StorageIO<DataFile> storageIO = null;

                try {
                    storageIO = doomed.getStorageIO();
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to initialize physical access driver.", ioex, this);
                }

                if (storageIO != null) {

                    // First, delete all the derivative files:
                    // We may have a few extra files associated with this object - 
                    // preserved original that was used in the tabular data ingest, 
                    // cached R data frames, image thumbnails, etc.
                    // We need to delete these too; failures however are less 
                    // important with these. If we fail to delete any of these 
                    // auxiliary files, we'll just leave an error message in the 
                    // log file and proceed deleting the database object.
                    try {
                        storageIO.open();
                    } catch (IOException ioex) {
                        Logger.getLogger(DeleteDataFileCommand.class.getName()).log(Level.SEVERE, "Error calling storageIO.open() while deleting DataFile {0}", doomed.getStorageIdentifier());
                    }
                    // Previously, storageIO.open() and storageIO.deleteAllAuxObjects() were in the same try/catch block
                    // but this was preventing the auxillary objects from being deleted when the main file isn't present
                    // on disk/storage. The call to open() was throwing an IOException so the deleteAllAuxObjects() method
                    // never fired. Now we have them in separate try/catch blocks to go ahead and delete the auxillary
                    // objects even if the main file can't be opened.
                    try {
                        storageIO.deleteAllAuxObjects();
                    } catch (IOException ioex) {
                        Logger.getLogger(DeleteDataFileCommand.class.getName()).log(Level.SEVERE, "Error calling storageIO.deleteAllAuxObjects() while deleting DataFile {0}", doomed.getStorageIdentifier());
                    }

                    // We only want to attempt to delete the main physical file
                    // if it actually exists, on the filesystem or whereever it 
                    // is actually stored by its StorageIO:
                    boolean physicalFileExists = false;

                    try {
                        physicalFileExists = storageIO.exists();
                    } catch (IOException ioex) {
                        // We'll assume that an exception here means that the file does not
                        // exist; so we can skip trying to delete it. 
                        physicalFileExists = false;
                    }

                    
                    if (physicalFileExists) {
                        try {
                            storageIO.delete();
                        } catch (IOException ex) {
                            // This we will treat as a fatal condition:
                            throw new CommandExecutionException("Error deleting physical file object while deleting DataFile " + doomed.getId() + " from the database.", ex, this);
                        }
                    }

                    logger.log(Level.FINE, "Successfully deleted physical storage object (file) for the DataFile {0}", doomed.getId());
                    // Destroy the storageIO object - we will need to purge the 
                    // DataFile from the database (below), so we don't want to have any
                    // objects in this transaction that reference it:
                    storageIO = null;
                    
                }
                */
            }
        }
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
        try {
            if (idServiceBean.alreadyExists(doomed)) {
                idServiceBean.deleteIdentifier(doomed);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier deletion was not successfull:", e.getMessage());
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
         * target individual files for deletion, which should always be drafts.
         *
         * See also https://redmine.hmdc.harvard.edu/issues/3786
         */
        String indexingResult = ctxt.index().removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierFile + doomed.getId() + "_draft");
        /**
         * @todo check indexing result for success or failure. Really, we need
         * an indexing queuing system:
         * https://redmine.hmdc.harvard.edu/issues/3643
         */

    }
    
    @Override 
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.describe());
        sb.append("DataFile:");
        sb.append(doomed.getId());
        sb.append(" ");
        return sb.toString();
    }

}
