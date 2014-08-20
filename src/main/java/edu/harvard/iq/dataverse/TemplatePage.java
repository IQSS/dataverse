package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDateverseTemplateCommand;
import java.sql.Timestamp;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

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

    @Inject
    DataverseSession session;

    public enum EditMode {

        CREATE, METADATA
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

    public void init() {
        if (templateId != null) { // edit or view existing for a template  
            dataverse = dataverseService.find(ownerId);
            for (Template dvTemp : dataverse.getTemplates()) {
                if (dvTemp.getId().longValue() == templateId) {
                    template = dvTemp;
                }
            }
            template.setDataverse(dataverse);
            template.setUsageCount(new Long(0));
            template.setMetadataValueBlocks();
        } else if (ownerId != null) {
            // create mode for a new template
            dataverse = dataverseService.find(ownerId);
            editMode = TemplatePage.EditMode.CREATE;
            template = new Template(this.dataverse);
        } else {
            throw new RuntimeException("On Template page without id or ownerid."); // improve error handling
        }
    }

    public void edit(TemplatePage.EditMode editMode) {
        this.editMode = editMode;
    }

    public String save() {

        // Use the API to save the dataset: 
        Command<Dataverse> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                template.setCreateTime(new Timestamp(new Date().getTime()));
                template.setUsageCount(new Long(0));
                dataverse.getTemplates().add(template);
                cmd = new UpdateDataverseCommand(dataverse, null, null, session.getUser());
                commandEngine.submit(cmd);
            } else {
                cmd = new UpdateDateverseTemplateCommand(dataverse, template, session.getUser());
                commandEngine.submit(cmd);
            }

        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex + " ");
            error.append(ex.getMessage() + " ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause + " ");
                error.append(cause.getMessage() + " ");
            }
            //
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Template Save Failed", " - " + error.toString()));
            System.out.print("dataverse " + dataverse.getName());
            System.out.print("Ejb exception");
            System.out.print(error.toString());
            return null;
        } catch (CommandException ex) {
            System.out.print("command exception");
            System.out.print(ex.toString());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Template Save Failed", " - " + ex.toString()));
            //logger.severe(ex.getMessage());
        }

        editMode = null;

        return "/template.xhtml?id=" + template.getId() + "&ownerId=" + dataverse.getId() + "&faces-redirect=true";
    }

    public void cancel() {
        editMode = null;
    }

}
