/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.primefaces.component.tabview.TabView;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author ellenk
 */
@ViewScoped
@Named
public class ThemeWidgetFragment implements java.io.Serializable {
    static final String DEFAULT_LOGO_BACKGROUND_COLOR = "F5F5F5";
    static final String DEFAULT_BACKGROUND_COLOR = "F5F5F5";
    static final String DEFAULT_LINK_COLOR = "428BCA";
    static final String DEFAULT_TEXT_COLOR = "888888";
     
    @Inject DataversePage dataversePage;
    private File tempDir;
    private File uploadedFile;
    private Dataverse editDv;
    private TabView tabView;
      @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DataverseServiceBean dataverseServiceBean;
    /**
     *     create tempDir, needs to be under docroot so that uploaded image is accessible in the page
     */
  
    private boolean testVal;

    public boolean isTestVal() {
        System.out.println("getting testVal: "+testVal);
        return testVal;
    }

    public void setTestVal(boolean testVal) {
        System.out.println("setting testVal: "+testVal);
        this.testVal = testVal;
    }
    
    public void testValListener(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        System.out.println("listener clicked, testVal: "+testVal);
    }
    
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
     *  Cleanup by deleting temp directory and uploaded files  
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
    
    public void checkboxListener() {
        System.out.println("checkbox clicked, themeRoot value: "+editDv.getThemeRoot());
    }

    public void initEditDv(Long dataverseId) {
        editDv = dataverseServiceBean.find(dataverseId);
        if (editDv.getOwner()==null) {
            editDv.setThemeRoot(true);
        }
        if (editDv.getDataverseTheme()==null && editDv.isThemeRoot()) {
            editDv.setDataverseTheme(initDataverseTheme());
            
        }
        // When you open the popup, the first tab (widgets) should be active
        tabView.setActiveIndex(0);
    }
    
    private DataverseTheme initDataverseTheme() {
        DataverseTheme dvt = new DataverseTheme();
        dvt.setLinkColor(DEFAULT_LINK_COLOR);
        dvt.setLogoBackgroundColor(DEFAULT_LOGO_BACKGROUND_COLOR);
        dvt.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        dvt.setTextColor(DEFAULT_TEXT_COLOR);
        return dvt;
    }
    
    public Dataverse getEditDv() {
        return editDv; 
    }

    public void setEditDv(Dataverse editDV) {
         this.editDv = editDV;
      
          
    }

public void validateUrl(FacesContext context, UIComponent component, Object value) throws ValidatorException {
    try {
        if (!StringUtils.isEmpty((String)value)){
            URL test = new URL((String)value);
        }
    } catch(MalformedURLException e) {
        System.out.println("url validation failed.");
        FacesMessage msg =
              new FacesMessage(" URL validation failed.",
              "Please provide URL.");
      msg.setSeverity(FacesMessage.SEVERITY_ERROR);
    
      throw new ValidatorException(msg);
    }
    
  }
    public TabView getTabView() {
        return tabView;
    }

    public void setTabView(TabView tabView) {
        this.tabView = tabView;
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
            editDv.getDataverseTheme().setLogo(uFile.getFileName());

        } catch (IOException e) {
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
        // If needed, set the default values for the logo
        if (editDv.getDataverseTheme().getLogoFormat()==null) {
            editDv.getDataverseTheme().setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
        }
        // Set the active index, so that Theme tab will still display after upload
        tabView.setActiveIndex(0);

    }
    
    public void removeLogo() {
        editDv.getDataverseTheme().setLogo(null);
        this.cleanupTempDirectory();
       
    }

    public boolean getInheritCustomization() {
        boolean inherit= editDv==null ? true : !editDv.getThemeRoot();
        System.out.println("returning inherit: "+inherit);
        return inherit;
    }
    
    public void setInheritCustomization(boolean inherit) {
        System.out.println("setting inherit : "+inherit+", themeRoot = "+!inherit);
        editDv.setThemeRoot(!inherit);
        if (!inherit) {
            if (editDv.getDataverseTheme(true)==null) {
                editDv.setDataverseTheme(initDataverseTheme());
            }
        }
    }

    public void save() {
        // If this Dv isn't the root, delete the uploaded file and remove theme
        // before saving.
        if (!editDv.isThemeRoot()) {
            uploadedFile=null;
            editDv.setDataverseTheme(null);
        }
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



