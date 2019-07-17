package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
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

    private static final Logger logger = Logger.getLogger(TemplatePage.class.getCanonicalName());

    public enum EditMode {
        CREATE, METADATA
    }

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
        if (templateId != null) { // edit existing template
            editMode = TemplatePage.EditMode.METADATA;
            template = templateService.find(templateId);
            template.setDataverse(dataverse);
            template.setMetadataValueBlocks();

            if (template.getTermsOfUseAndAccess() == null) {
                TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
                terms.setTemplate(template);
                terms.setLicense(TermsOfUseAndAccess.License.CC0);
                template.setTermsOfUseAndAccess(terms);
            }

            updateDatasetFieldsIncludeFlag();
        } else if (ownerId != null) {
            // create mode for a new template

            editMode = TemplatePage.EditMode.CREATE;
            template = new Template(this.dataverse);
            TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
            terms.setTemplate(template);
            terms.setLicense(TermsOfUseAndAccess.License.CC0);
            template.setTermsOfUseAndAccess(terms);
            updateDatasetFieldsIncludeFlag();
        } else {
            throw new RuntimeException("On Template page without id or ownerid."); // improve error handling
        }
        return null;
    }

    private void updateDatasetFieldsIncludeFlag() {
        Dataverse owner = dataverseService.find(ownerId);
        Long dvIdForInputLevel = owner.getMetadataRootId();

        for (DatasetField dsf : template.getFlatDatasetFields()) {
            DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dvIdForInputLevel, dsf.getDatasetFieldType().getId());
            if (dsfIl != null) {
                dsf.setInclude(dsfIl.isInclude());
            } else {
                dsf.setInclude(true);
            }
        }
    }

    public String save() {

        Command<Void> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                template.setCreateTime(new Timestamp(new Date().getTime()));
                template.setUsageCount(0L);
                dataverse.getTemplates().add(template);
                commandEngine.submit(new CreateTemplateCommand(template, dvRequestService.getDataverseRequest(), dataverse));
                
                JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.create"));
            } else {
                cmd = new UpdateDataverseTemplateCommand(dataverse, template, dvRequestService.getDataverseRequest());
                commandEngine.submit(cmd);
                JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.save"));
            }

        } catch (EJBException | CommandException ex) {

            logger.fine("There was a problem with creating template: " + ex.getMessage());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("template.save.fail"));
            return StringUtils.EMPTY;
        }
        

        return "/manage-templates.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";
    }

}
