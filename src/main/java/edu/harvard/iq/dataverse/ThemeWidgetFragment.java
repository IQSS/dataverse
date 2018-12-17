/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.primefaces.context.RequestContext;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author ellenk
 */
@ViewScoped
@Named
public class ThemeWidgetFragment implements java.io.Serializable {
    static final String DEFAULT_LOGO_BACKGROUND_COLOR = "FFFFFF";
    static final String DEFAULT_BACKGROUND_COLOR = "FFFFFF";
    static final String DEFAULT_LINK_COLOR = "428BCA";
    static final String DEFAULT_TEXT_COLOR = "888888";
    private static final Logger logger = Logger.getLogger(ThemeWidgetFragment.class.getCanonicalName());   


    private File tempDir;
    private File uploadedFile;
    private Dataverse editDv= new Dataverse();
    private HtmlInputText linkUrlInput;
    private HtmlInputText taglineInput;
 

    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DataverseServiceBean dataverseServiceBean;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    

    @Inject PermissionsWrapper permissionsWrapper;
    
    
    public HtmlInputText getLinkUrlInput() {
        return linkUrlInput;
    }

    public void setLinkUrlInput(HtmlInputText linkUrlInput) {
        this.linkUrlInput = linkUrlInput;
    }

    public HtmlInputText getTaglineInput() {
        return taglineInput;
    }

    public void setTaglineInput(HtmlInputText taglineInput) {
        this.taglineInput = taglineInput;
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
        // not sure if this is needed for the ajax component
    }
   

    public String initEditDv() {
        editDv = dataverseServiceBean.find(editDv.getId());
        
        // check if dv exists and user has permission
        if (editDv == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(editDv, UpdateDataverseThemeCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }        
        
        
        if (editDv.getOwner()==null) {
            editDv.setThemeRoot(true);
        }
        if (editDv.getDataverseTheme()==null && editDv.isThemeRoot()) {
            editDv.setDataverseTheme(initDataverseTheme());
            
        }
        return null;
     }
    
    private DataverseTheme initDataverseTheme() {
        DataverseTheme dvt = new DataverseTheme();
        dvt.setLinkColor(DEFAULT_LINK_COLOR);
        dvt.setLogoBackgroundColor(DEFAULT_LOGO_BACKGROUND_COLOR);
        dvt.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        dvt.setTextColor(DEFAULT_TEXT_COLOR);
        dvt.setDataverse(editDv);
        return dvt;
    }
    
    public Dataverse getEditDv() {
        return editDv; 
    }

    public void setEditDv(Dataverse editDV) {
         this.editDv = editDV;
      
          
    }
    public void validateTagline(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (!StringUtils.isEmpty((String) value) && ((String) value).length() > 140) {
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("theme.validateTagline"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }

    }
    
    public void validateUrl(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        try {
            if (!StringUtils.isEmpty((String) value)) {
                URL test = new URL((String) value);
            }
        } catch (MalformedURLException e) {
            FacesMessage msg
                    = new FacesMessage(BundleUtil.getStringFromBundle("theme.urlValidate"),
                    BundleUtil.getStringFromBundle("theme.urlValidate.msg"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }
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

            logger.finer("entering fileUpload");
            if (this.tempDir==null) {
                createTempDir();
                logger.finer("created tempDir");
            }
            UploadedFile uFile = event.getFile();
        try {         
            uploadedFile = new File(tempDir, uFile.getFileName());     
            if (!uploadedFile.exists()) {
                uploadedFile.createNewFile();
            }
            logger.finer("created file");
            Files.copy(uFile.getInputstream(), uploadedFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            logger.finer("copied inputstream to file");
            editDv.getDataverseTheme().setLogo(uFile.getFileName());

        } catch (IOException e) {
            logger.finer("caught IOException");
            logger.throwing("ThemeWidgetFragment", "handleImageFileUpload", e);
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
        // If needed, set the default values for the logo
        if (editDv.getDataverseTheme().getLogoFormat()==null) {
            editDv.getDataverseTheme().setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
        }
        logger.finer("end handelImageFileUpload");
    }
    
    public void removeLogo() {
        editDv.getDataverseTheme().setLogo(null);
        this.cleanupTempDirectory();
       
    }

    public boolean getInheritCustomization() {
        boolean inherit= editDv==null ? true : !editDv.getThemeRoot();
         return inherit;
    }
    
    public void setInheritCustomization(boolean inherit) {
        editDv.setThemeRoot(!inherit);
        if (!inherit) {
            if (editDv.getDataverseTheme(true)==null) {
                editDv.setDataverseTheme(initDataverseTheme());
            }
        }
    }
    public void resetForm() {
        RequestContext context = RequestContext.getCurrentInstance();
        context.reset(":dataverseForm:themeWidgetsTabView");
    }
    
    public String cancel() {
         return "dataverse.xhtml?faces-redirect=true&alias="+editDv.getAlias();  // go to dataverse page 
    }
    
    
    public String save() {
        // If this Dv isn't the root, delete the uploaded file and remove theme
        // before saving.
        if (!editDv.isThemeRoot()) {
            uploadedFile=null;
            editDv.setDataverseTheme(null);
        }
        Command<Dataverse>    cmd = new UpdateDataverseThemeCommand(editDv, this.uploadedFile, dvRequestService.getDataverseRequest());
        try {
            commandEngine.submit(cmd);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "error updating dataverse theme", ex);
           FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataverse.save.failed"), BundleUtil.getStringFromBundle("dataverse.theme.failure")));
        
          return null;
        } finally {
              this.cleanupTempDirectory(); 
        }
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.theme.success"));    
        return "dataverse.xhtml?faces-redirect=true&alias="+editDv.getAlias();  // go to dataverse page 
    }
      
 }



