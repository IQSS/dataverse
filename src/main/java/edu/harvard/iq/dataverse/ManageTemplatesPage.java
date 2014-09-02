package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateRootCommand;
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

/**
 *
 * @author skraffmiller
 */
@ViewScoped
@Named
public class ManageTemplatesPage {

    @EJB
    DataverseServiceBean dvService;

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

    private List<Template> templates;
    private Dataverse dataverse;
    private Long dataverseId;
    private boolean inheritTemplatesValue;
    private Template selectedTemplate = null;

    public void init() {
        dataverse = dvService.find(dataverseId);
        dvpage.setDataverse(dataverse);
        templates = new LinkedList<>();
        setInheritTemplatesValue(dataverse.isTemplateRoot());
        if (inheritTemplatesValue && dataverse.getOwner() != null) {
            for (Template pt : dataverse.getParentTemplates()) {
                pt.setDataverse(dataverse.getOwner());
                templates.add(pt);
            }
        }
        for (Template ct : dataverse.getTemplates()) {
            ct.setDataverse(dataverse);
            templates.add(ct);
        }

    }

    public void makeDefault(Template templateIn) {
        dataverse.setDefaultTemplate(templateIn);
        saveDataverse("Dataverse Default Template updated");
    }

    public void unselectDefault(Template templateIn) {
        dataverse.setDefaultTemplate(null);
        saveDataverse("Dataverse Default Template updated");
    }

    public String cloneTemplate(Template templateIn) {
        Template newOne = templateIn.cloneNewTemplate(templateIn);
        String name = "Copy of " + templateIn.getName();
        newOne.setName(name);
        newOne.setUsageCount(new Long(0));
        newOne.setCreateTime(new Timestamp(new Date().getTime()));
        dataverse.getTemplates().add(newOne);
        templates.add(newOne);
        Template created;
        try {
            created = engineService.submit(new CreateTemplateCommand(newOne, session.getUser(), dataverse));
            saveDataverse("");
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Dataverse Template created");
            created.setDataverse(dataverse);
            return "/template.xhtml?id=" + created.getId() + "&ownerId=" + dataverse.getId() + "&editMode=METADATA&faces-redirect=true";
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Update failed: " + ex.getMessage());
        }
        return "";
    }

    public void deleteTemplate() {
        if (selectedTemplate != null) {
            templates.remove(selectedTemplate);
            dataverse.getTemplates().remove(selectedTemplate);
            saveDataverse("Template deleted from dataverse");
        } else {
            System.out.print("selected template is null");
        }
    }

    public void saveDataverse(ActionEvent e) {
        saveDataverse("");
    }

    private void saveDataverse(String successMessage) {
        if (successMessage.isEmpty()) {
            successMessage = "Dataverse Template data updated";
        }
        try {
            engineService.submit(new UpdateDataverseCommand(getDataverse(), null, null, session.getUser()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, successMessage);
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Update failed: " + ex.getMessage());
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
            }
            dataverse = engineService.submit(new UpdateDataverseTemplateRootCommand(isInheritTemplatesValue(), session.getUser(), getDataverse()));
            init();
            return "";
        } catch (CommandException ex) {
            Logger.getLogger(ManageTemplatesPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
