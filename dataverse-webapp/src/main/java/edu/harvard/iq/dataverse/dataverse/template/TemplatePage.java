package edu.harvard.iq.dataverse.dataverse.template;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 * @author skraffmiller
 */
@ViewScoped
@Named("TemplatePage")
public class TemplatePage implements java.io.Serializable {

    @EJB
    TemplateDao templateDao;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    @Inject
    PermissionsWrapper permissionsWrapper;

    @Inject
    DatasetFieldsInitializer datasetFieldsInitializer;

    @Inject
    private TemplateService templateService;

    private static final Logger logger = Logger.getLogger(TemplatePage.class.getCanonicalName());

    public enum EditMode {
        CREATE, METADATA
    }

    private Template template;
    private Dataverse dataverse;
    private EditMode editMode;
    private Long ownerId;
    private Long templateId;
    private Map<MetadataBlock, List<DatasetField>> mdbForEdit;

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

    public Map<MetadataBlock, List<DatasetField>> getMdbForEdit() {
        return mdbForEdit;
    }

    public String init() {

        if (isEditingTemplate()) {
            editMode = TemplatePage.EditMode.METADATA;
            template = templateDao.find(templateId);

            dataverse = template.getDataverse();

            if (dataverse == null) {
                return permissionsWrapper.notFound();
            }

            if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
                return permissionsWrapper.notAuthorized();
            }

            List<DatasetField> dsfForEdit = datasetFieldsInitializer.prepareDatasetFieldsForEdit(template.getDatasetFields(), dataverse.getMetadataBlockRootDataverse());
            template.setDatasetFields(dsfForEdit);
            mdbForEdit = datasetFieldsInitializer.groupAndUpdateEmptyAndRequiredFlag(dsfForEdit);

            if (template.getTermsOfUseAndAccess() == null) {
                template.setTermsOfUseAndAccess(prepareTermsOfUseAndAccess(template));
            }


        } else if (isCreatingTemplate()) {
            dataverse = dataverseService.find(ownerId);

            if (dataverse == null) {
                return permissionsWrapper.notFound();
            }

            if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
                return permissionsWrapper.notAuthorized();
            }

            editMode = TemplatePage.EditMode.CREATE;
            template = new Template(this.dataverse);

            template.setTermsOfUseAndAccess(prepareTermsOfUseAndAccess(template));

            List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(template.getDatasetFields(), dataverse.getMetadataBlockRootDataverse());
            template.setDatasetFields(datasetFields);
            mdbForEdit = datasetFieldsInitializer.groupAndUpdateEmptyAndRequiredFlag(datasetFields);
        } else {
            throw new RuntimeException("On Template page without id or ownerid."); // improve error handling
        }

        return StringUtils.EMPTY;
    }

    public String save() {

        Try<Template> templateOperation;

        if (editMode == EditMode.CREATE) {

            templateOperation = templateService.createTemplate(dataverse, this.template)
                    .onSuccess(op -> JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.create")));
        } else {

            templateOperation = templateService.updateTemplate(dataverse, template)
                    .onSuccess(op -> JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("template.save")));
        }

        if (templateOperation.isFailure()) {
            logger.fine("There was a problem with creating template: " + templateOperation.getCause().getMessage());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("template.save.fail"));

            return StringUtils.EMPTY;
        }

        return "/manage-templates.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";
    }

    private TermsOfUseAndAccess prepareTermsOfUseAndAccess(Template template) {
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setTemplate(template);
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
        return terms;
    }

    private boolean isEditingTemplate() {
        return templateId != null;
    }

    private boolean isCreatingTemplate() {
        return ownerId != null;
    }

}
