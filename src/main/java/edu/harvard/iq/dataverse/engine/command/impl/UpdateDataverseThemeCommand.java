package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.ThemeWidgetFragment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Update an existing dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateDataverseThemeCommand extends AbstractCommand<Dataverse> {
    private final Dataverse editedDv;
    private final File uploadedFile;
    private String locate;

    public UpdateDataverseThemeCommand(Dataverse editedDv, File uploadedFile, DataverseRequest aRequest, String location) {
        super(aRequest, editedDv);
        this.uploadedFile = uploadedFile;
        this.editedDv = editedDv;
        this.locate = location;

    }
    /**
     * Update Theme and Widget related data for this dataverse, and 
     * do file management needed for theme images.
     * 
     * @param ctxt
     * @return
     * @throws CommandException 
     */
    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        // Get current dataverse, so we can delete current logo file if necessary
        Dataverse currentDv = ctxt.dataverses().find(editedDv.getId());
        File logoFileDir = ThemeWidgetFragment.getLogoDir(editedDv.getId().toString()).toFile();
        File currentFile=null;

        if (locate.equals("FOOTER")){
            if (currentDv.getDataverseTheme()!=null && currentDv.getDataverseTheme().getLogoFooter()!=null) {
                currentFile = new File(logoFileDir, currentDv.getDataverseTheme().getLogoFooter());
            }
            try {
                // If edited logo field is empty, and a logoFile currently exists, delete it
                if (editedDv.getDataverseTheme()==null || editedDv.getDataverseTheme().getLogoFooter()==null ) {
                    if (currentFile!=null) {
                        currentFile.delete();
                    }
                } // If edited logo file isn't empty,and uploaded File exists, delete currentFile and copy uploaded file from temp dir to logos dir
                else if (uploadedFile!=null) {
                    File newFile = new File(logoFileDir,editedDv.getDataverseTheme().getLogoFooter());
                    if (currentFile!=null) {
                        currentFile.delete();
                    }
                    logoFileDir.mkdirs();
                    Files.copy(uploadedFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (IOException e) {
                throw new CommandException("Error saving logo footer file", e,this); // improve error handling

            }
        } else if (locate.equals("HEADER")){
            if (currentDv.getDataverseTheme()!=null && currentDv.getDataverseTheme().getLogo()!=null) {
                currentFile = new File(logoFileDir, currentDv.getDataverseTheme().getLogo());
            }
            try {
                // If edited logo field is empty, and a logoFile currently exists, delete it
                if (editedDv.getDataverseTheme()==null || editedDv.getDataverseTheme().getLogo()==null ) {
                    if (currentFile!=null) {
                        currentFile.delete();
                    }
                } // If edited logo file isn't empty,and uploaded File exists, delete currentFile and copy uploaded file from temp dir to logos dir
                else if (uploadedFile!=null) {
                    File newFile = new File(logoFileDir,editedDv.getDataverseTheme().getLogo());
                    if (currentFile!=null) {
                        currentFile.delete();
                    }
                    logoFileDir.mkdirs();
                    Files.copy(uploadedFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new CommandException("Error saving logo file", e,this); // improve error handling

            }
        }
        // save updated dataverse to db
        return ctxt.dataverses().save(editedDv);
    }

}
