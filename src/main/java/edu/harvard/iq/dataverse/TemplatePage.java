package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateCommand;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * @author skraffmiller
 */
@ViewScoped
@Named("TemplatePage")
public class TemplatePage implements java.io.Serializable {

    @EJB
    TemplateServiceBean templateService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    EjbDataverseEngine commandEngine;
    
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService; 
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    @Inject
    PermissionsWrapper permissionsWrapper;
    
    @Inject
    DataverseSession session;
    
    @Inject
    LicenseServiceBean licenseServiceBean;
    
    @Inject
    SettingsWrapper settingsWrapper;
    
    private static final Logger logger = Logger.getLogger(TemplatePage.class.getCanonicalName());

    public enum EditMode {

        CREATE, METADATA, LICENSE, LICENSEADD, CLONE
    };

    private Template template;
    private Dataverse dataverse;
    private EditMode editMode;
    private Long ownerId;
    private Long templateId;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    private int selectedTabIndex;

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public String init() {
 
        dataverse = dataverseService.find(ownerId);
        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        } 
        if (templateId != null) { // edit or view existing for a template  

            template = templateService.find(templateId);
            template.setDataverse(dataverse);
            template.setMetadataValueBlocks(settingsWrapper.getSystemMetadataBlocks());

            if (template.getTermsOfUseAndAccess() != null) {
                TermsOfUseAndAccess terms = template.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
                terms.setTemplate(template);
                template.setTermsOfUseAndAccess(terms);
            }

            updateDatasetFieldInputLevels();
        } else if (ownerId != null) {
            // create mode for a new template

            editMode = TemplatePage.EditMode.CREATE;
            template = new Template(this.dataverse, settingsWrapper.getSystemMetadataBlocks());
            TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
            terms.setFileAccessRequest(true);
            terms.setTemplate(template);
            terms.setLicense(licenseServiceBean.getDefault());
            template.setTermsOfUseAndAccess(terms);
            updateDatasetFieldInputLevels();
        } else {
            throw new RuntimeException("On Template page without id or ownerid."); // improve error handling
        }       
        return null;        
    }
    
    private void updateDatasetFieldInputLevels(){
        Long dvIdForInputLevel = ownerId;        
        if (!dataverseService.find(ownerId).isMetadataBlockRoot()){
            dvIdForInputLevel = dataverseService.find(ownerId).getMetadataRootId();
        }        
        
        for (DatasetField dsf: template.getFlatDatasetFields()){ 
           DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dvIdForInputLevel, dsf.getDatasetFieldType().getId());
           if (dsfIl != null){
               dsf.setInclude(dsfIl.isInclude());
           } else {
               dsf.setInclude(true);
           } 
        }
    }

    public void edit(TemplatePage.EditMode editMode) {
        this.editMode = editMode;
    }

    public String save(String redirectPage) {
        
        boolean create = false;
        Command<Void> cmd;
        Long createdId = new Long(0);
        Template created;
        try {

            DatasetFieldUtil.tidyUpFields( template.getDatasetFields(), false );

            template.updateInstructions();
            
            if (editMode == EditMode.CREATE) {
                template.setCreateTime(new Timestamp(new Date().getTime()));
                template.setUsageCount(new Long(0));
                dataverse.getTemplates().add(template);
                created = commandEngine.submit(new CreateTemplateCommand(template, dvRequestService.getDataverseRequest(), dataverse));
                createdId = created.getId();
                //cmd = new UpdateDataverseCommand(dataverse, null, null, dvRequestService.getDataverseRequest(), null);
                create = true;
                //commandEngine.submit(cmd);
            } else {
                cmd = new UpdateDataverseTemplateCommand(dataverse, template, dvRequestService.getDataverseRequest());
                commandEngine.submit(cmd);
            }

        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            logger.warning("Template Save failed - Ejb exception " + error.toString());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("template.save.fail"));
            return null;
        } catch (CommandException ex) {
            logger.severe("Template Save failed - Ejb exception " + ex.toString());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("template.save.fail"));
            return null;
        }
        editMode = null;       
        String msg = (create)? BundleUtil.getStringFromBundle("template.create"): BundleUtil.getStringFromBundle("template.save");
        JsfHelper.addFlashMessage(msg);
        String retString = "";   
        if (!redirectPage.isEmpty() && createdId.intValue() > 0) {
            retString = "/template.xhtml?id=" + createdId + "&ownerId=" + dataverse.getId() + "&editMode=LICENSEADD&faces-redirect=true";
        } else {
            retString = "/manage-templates.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";           
        }
        return retString;
    }

    public void cancel() {
        editMode = null;
    }
    
    public String deleteTemplate(Long templateId) {
        List <Dataverse> dataverseWDefaultTemplate = null;
        Template doomed = templateService.find(templateId);
        dataverse.getTemplates().remove(doomed);  
        dataverseWDefaultTemplate = templateService.findDataversesByDefaultTemplateId(doomed.getId());
        try {
            commandEngine.submit(new DeleteTemplateCommand(dvRequestService.getDataverseRequest(), getDataverse(), doomed, dataverseWDefaultTemplate  ));
            JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.delete"));//("The template has been deleted");
        } catch (CommandException ex) {
            String failMessage = BundleUtil.getStringFromBundle("template.delete.error");//"The dataset template cannot be deleted.";
            JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
        }
        return "/manage-templates.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true"; 
    }
    
    //Get the cutstom instructions defined for a give fieldType
    public String getInstructionsLabelFor(String fieldType) {
        String fieldInstructions = template.getInstructionsMap().get(fieldType);
        return (fieldInstructions!=null && !fieldInstructions.isBlank()) ? fieldInstructions : BundleUtil.getStringFromBundle("template.instructions.empty.label");
    }

}
