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
        setInheritTemplatesValue(!dataverse.isTemplateRoot());
        if (inheritTemplatesValue && dataverse.getOwner() != null) {
            for (Template pt : dataverse.getOwner().getTemplates()) {
                pt.setDataverse(dataverse.getOwner());
                templates.add(pt);
            }
        }
        for (Template ct : dataverse.getTemplates(true)) {
            ct.setDataverse(dataverse);
            templates.add(ct);
        }
    }

    public void makeDefault(Template templateIn) {
        dataverse.setDefaultTemplate(templateIn);
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
            saveDataverse();
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
            saveDataverse();
        } else {
            System.out.print("selected template is null");
        }
    }

    public void saveDataverse(ActionEvent e) {
        saveDataverse();
    }

    private void saveDataverse() {
        try {
            engineService.submit(new UpdateDataverseCommand(getDataverse(), null, null, session.getUser()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Dataverse Template data updated");
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

    public String updateTemplatesRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            dataverse = engineService.submit(new UpdateDataverseTemplateRootCommand(!isInheritTemplatesValue(), session.getUser(), getDataverse()));
            setInheritTemplatesValue(!dataverse.isTemplateRoot());
            init();
            return "";
        } catch (CommandException ex) {
            Logger.getLogger(ManageTemplatesPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
