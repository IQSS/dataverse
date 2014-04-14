package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * Deletes a data file, both DB entity and filesystem object.
 * @author michael
 */
@RequiredPermissions( Permission.DestructiveEdit )
public class DeleteDataFileCommand extends AbstractVoidCommand {
	
	private final DataFile doomed;

	public DeleteDataFileCommand(DataFile doomed, DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		this.doomed = doomed;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		if ( doomed.isReleased() ) {
			throw new IllegalCommandException("Cannot delete a released file", this);
		}
		                
		// We need to delete a bunch of files from the file system;
        // First we try to delete the data file itself; if that 
        // fails, we throw an exception and abort the command without
        // trying to remove the object from the database:

        String fileSystemName = doomed.getFileSystemName();
        if ( fileSystemName != null ) {
            Path filePath = Paths.get(fileSystemName);
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException ex) {
                    throw new CommandExecutionException("Error deleting physical file '" + doomed.getFileSystemLocation() + "' while deleting DataFile " + doomed.getName(), ex, this );
                }
            }                


            // We may also have a few extra files associated with this object - 
            // preserved original that was used in the tabular data ingest, 
            // cached R data frames, image thumbnails, etc.
            // We need to delete these too; failures however are less 
            // important with these. If we fail to delete any of these 
            // auxiliary files, we'll just leave an error message in the 
            // log file and proceed deleting the database object.

            List<Path> victims = new ArrayList<>(); 

            // 1. preserved original: 
            filePath = doomed.getSavedOriginalFile();
            if (filePath != null) {
                victims.add(filePath);
            }

            // 2. Cached files: 
            victims.addAll(listCachedFiles(doomed));

            // Delete them all: 
            List<String> failures = new ArrayList<>(); 
            for (Path deadFile : victims) {
                try {
                    Files.delete(deadFile);
                } catch (IOException ex) {
                    failures.add(deadFile.toString());
                }
            }

            if (!failures.isEmpty()) {
                String failedFiles = StringUtils.join(failures, ",");
                Logger.getLogger(DeleteDataFileCommand.class.getName()).log(Level.SEVERE,"Error deleting physical file(s) " + failedFiles + " while deleting DataFile " + doomed.getName());
            }

            // Finally, delete the file from the DB.
            ctxt.em().remove(doomed);
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
                    return (file.getFileName() != null &&
                            file.getFileName().toString().startsWith(baseName+"."));
                }
            };
            
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(datasetDirectory, filter)) {
                for (Path filePath : dirStream) {
                    victims.add(filePath);
                }
            } catch (IOException ex) {}
            
            return victims; 
        }
	
}
