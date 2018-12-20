package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateRootCommand;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import edu.harvard.iq.dataverse.util.BundleUtil;
/**
 *
 * @author skraffmiller
 */
@ViewScoped
@Named
public class ManageTemplatesPage implements java.io.Serializable {

    @EJB
    DataverseServiceBean dvService;

    @EJB
    TemplateServiceBean templateService;
    
    @EJB
    EjbDataverseEngine engineService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataversePage dvpage;

    @Inject
    TemplatePage tempPage;

    @Inject
    DataverseSession session;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    @Inject
    PermissionsWrapper permissionsWrapper;

    private List<Template> templates;
    private Dataverse dataverse;
    private Long dataverseId;
    private boolean inheritTemplatesValue;
    private boolean inheritTemplatesAllowed = false;

    private Template selectedTemplate = null;

    public String init() {
        dataverse = dvService.find(dataverseId);
        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }  
        dvpage.setDataverse(dataverse);
        if (dataverse.getOwner() != null && dataverse.getMetadataBlocks().equals(dataverse.getOwner().getMetadataBlocks())){
           setInheritTemplatesAllowed(true); 
        }
 
        templates = new LinkedList<>();
        setInheritTemplatesValue(!dataverse.isTemplateRoot());
        if (inheritTemplatesValue && dataverse.getOwner() != null) {
            for (Template pt : dataverse.getParentTemplates()) {
                pt.setDataverse(dataverse.getOwner());
                templates.add(pt);
            }
        }
        for (Template ct : dataverse.getTemplates()) {
            ct.setDataverse(dataverse);
            ct.setDataversesHasAsDefault(templateService.findDataversesByDefaultTemplateId(ct.getId()));
            ct.setIsDefaultForDataverse(!ct.getDataversesHasAsDefault().isEmpty());
            templates.add(ct);
        }
        if (!templates.isEmpty()){
             JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.manageTemplates.info.message.notEmptyTable"));
        }
        return null;
    }

    public void makeDefault(Template templateIn) {
        dataverse.setDefaultTemplate(templateIn);
        saveDataverse(BundleUtil.getStringFromBundle("template.makeDefault"));
    }

    public void unselectDefault(Template templateIn) {
        dataverse.setDefaultTemplate(null);
        saveDataverse(BundleUtil.getStringFromBundle("template.unselectDefault"));
    }

    public String cloneTemplate(Template templateIn) {
        Template newOne = templateIn.cloneNewTemplate(templateIn);
        String name = BundleUtil.getStringFromBundle("page.copy") +" " + templateIn.getName();
        newOne.setName(name);
        newOne.setUsageCount(new Long(0));
        newOne.setCreateTime(new Timestamp(new Date().getTime()));
        dataverse.getTemplates().add(newOne);
        templates.add(newOne);
        Template created;
        try {
            created = engineService.submit(new CreateTemplateCommand(newOne, dvRequestService.getDataverseRequest(), dataverse));
            saveDataverse("");
            String msg =  BundleUtil.getStringFromBundle("template.clone");//"The template has been copied";
            JsfHelper.addFlashMessage(msg);
            return "/template.xhtml?id=" + created.getId() + "&ownerId=" + dataverse.getId() + "&editMode=METADATA&faces-redirect=true";
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("template.clone.error"));//"Template could not be copied. " 
        }
        return "";
    }

    public void deleteTemplate() {
        List <Dataverse> dataverseWDefaultTemplate = null;
        if (selectedTemplate != null) {
            templates.remove(selectedTemplate);
            if(dataverse.getDefaultTemplate() != null && dataverse.getDefaultTemplate().equals(selectedTemplate)){
                dataverse.setDefaultTemplate(null);
            }
            dataverse.getTemplates().remove(selectedTemplate);  
            dataverseWDefaultTemplate = templateService.findDataversesByDefaultTemplateId(selectedTemplate.getId());
        } else {
            System.out.print("selected template is null");
        }
        try {
            engineService.submit(new DeleteTemplateCommand(dvRequestService.getDataverseRequest(), getDataverse(), selectedTemplate, dataverseWDefaultTemplate  ));
            JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.delete"));//("The template has been deleted");
        } catch (CommandException ex) {
            String failMessage = BundleUtil.getStringFromBundle("template.delete.error");//"The dataset template cannot be deleted.";
            JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
        }
    }

    public void saveDataverse(ActionEvent e) {
        saveDataverse("");
    }

    private void saveDataverse(String successMessage) {
        if (successMessage.isEmpty()) {
            successMessage = BundleUtil.getStringFromBundle("template.update");//"Template data updated";
        }
        try {
            engineService.submit(new UpdateDataverseCommand(getDataverse(), null, null, dvRequestService.getDataverseRequest(), null));
            //JH.addMessage(FacesMessage.SEVERITY_INFO, successMessage);
            JsfHelper.addFlashMessage(successMessage);
        } catch (CommandException ex) {
            String failMessage = BundleUtil.getStringFromBundle("template.update.error");//"Template update failed";
            if(successMessage.equals(BundleUtil.getStringFromBundle("template.delete"))){
                failMessage = BundleUtil.getStringFromBundle("template.delete.error");//"The dataset template cannot be deleted.";
            }
            if(successMessage.equals(BundleUtil.getStringFromBundle("template.makeDefault"))){
                failMessage = BundleUtil.getStringFromBundle("template.makeDefault.error");//"The dataset template cannot be made default.";
            }
            JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
        }

    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public boolean isInheritTemplatesValue() {
        return inheritTemplatesValue;
    }

    public void setInheritTemplatesValue(boolean inheritTemplatesValue) {
        this.inheritTemplatesValue = inheritTemplatesValue;
    }

    public boolean isInheritTemplatesAllowed() {
        return inheritTemplatesAllowed;
    }

    public void setInheritTemplatesAllowed(boolean inheritTemplatesAllowed) {
        this.inheritTemplatesAllowed = inheritTemplatesAllowed;
    }
    public Template getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(Template selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void viewSelectedTemplate(Template selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
        this.selectedTemplate.setMetadataValueBlocks();
        tempPage.setTemplate(selectedTemplate);
    }

    public String updateTemplatesRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            if (dataverse.getOwner() != null) {
                if (isInheritTemplatesValue() && dataverse.getDefaultTemplate() == null && dataverse.getOwner().getDefaultTemplate() != null) {
                    dataverse.setDefaultTemplate(dataverse.getOwner().getDefaultTemplate());
                }
                if (!isInheritTemplatesValue()) {
                    if (dataverse.getDefaultTemplate() != null) {
                        for (Template test : dataverse.getParentTemplates()) {
                            if (test.equals(dataverse.getDefaultTemplate())) {
                                dataverse.setDefaultTemplate(null);
                            }
                        }
                    }
                }
            }

            dataverse = engineService.submit(new UpdateDataverseTemplateRootCommand(!isInheritTemplatesValue(), dvRequestService.getDataverseRequest(), getDataverse()));
            init();
            return "";
        } catch (CommandException ex) {
            Logger.getLogger(ManageTemplatesPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
