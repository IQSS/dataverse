/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author ellenk
 */
@ViewScoped
@Named
public class ThemeWidgetFragment implements java.io.Serializable {

    @Inject DataversePage dataversePage;
    private File tempDir;
    private File uploadedFile;
    private String uploadedFileName;
    private Dataverse editDv;
      @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DataverseServiceBean dataverseServiceBean;
    /**
     *     create tempDir, needs to be under docroot so that uploaded image is accessible in the page
     */
  
    private  void createTempDir() {
          try {
            File tempRoot = Files.createDirectories(Paths.get("../docroot/logos/temp")).toFile();
            tempDir = Files.createTempDirectory(tempRoot.toPath(),editDv.getId().toString()).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Error creating temp directory", e); // improve error handling
        }
    }
    
    @PreDestroy
    /**
     *  Cleanup by deleting temp directory and uploaded file  
     */
    public void cleanupTempDirectory() {
        try {
           
            if (tempDir != null) {
                for (File f : tempDir.listFiles()) {
                    Files.deleteIfExists(f.toPath());
                }
                Files.deleteIfExists(tempDir.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error deleting temp directory", e); // improve error handling
        }
        uploadedFile=null;
        tempDir=null;
    }

    public void initEditDv(Long dataverseId) {
        editDv = dataverseServiceBean.find(dataverseId);
    }
    public Dataverse getEditDv() {
        return editDv; 
    }

    public void setEditDv(Dataverse editDV) {
         this.editDv = editDV;
      
          
    }
    
    public String getTempDirName() {
        if (tempDir!=null) {
            return tempDir.getName();
        } else {
            return null;
        }
    }
    
    public boolean uploadExists() {
        return uploadedFile!=null;
    }
    /**
     * Copy uploaded file to temp area, until we are ready to save
     * Copy filename into Dataverse logo 
     * @param event 
     */
    public void handleImageFileUpload(FileUploadEvent event) {
            if (this.tempDir==null) {
                createTempDir();
            }
            UploadedFile uFile = event.getFile();
        try {         
            uploadedFile = new File(tempDir, uFile.getFileName());     
            if (!uploadedFile.exists()) {
                uploadedFile.createNewFile();
            }
            Files.copy(uFile.getInputstream(), uploadedFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            editDv.setLogo(uFile.getFileName());

        } catch (IOException e) {
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
        // If needed, set the default values for the logo
        if (editDv.getLogoFormat()==null) {
            editDv.setLogoFormat(Dataverse.ImageFormat.SQUARE);
        }

    }
    
    public void removeLogo() {
        editDv.setLogo(null);
        this.cleanupTempDirectory();
       
    }

   

    public void save() {
        Command<Dataverse>    cmd = new UpdateDataverseThemeCommand(editDv, this.uploadedFile, session.getUser());  
        try {
            dataversePage.setDataverse(commandEngine.submit(cmd));           
            dataversePage.setEditMode(null);
            
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());          
        }
        this.cleanupTempDirectory();
       
    }
}



