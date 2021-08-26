package edu.harvard.iq.dataverse.dataverse.template;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRenderer;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRendererManager;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * @author skraffmiller
 */
@ViewScoped
@Named("TemplatePage")
public class TemplatePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(TemplatePage.class.getCanonicalName());

    public enum EditMode {
        CREATE, EDIT, CLONE
    }

    private EditMode editMode;
    private Long ownerId;
    private Long templateId;

    private Template template;
    private Dataverse dataverse;
    private Map<MetadataBlock, List<DatasetFieldsByType>> mdbForEdit;
    private Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();

    @EJB
    TemplateDao templateDao;

    @EJB
    DataverseDao dataverseDao;

    @Inject
    PermissionsWrapper permissionsWrapper;

    @Inject
    DatasetFieldsInitializer datasetFieldsInitializer;

    @Inject
    private TemplateService templateService;

    @EJB
    private InputFieldRendererManager inputFieldRendererManager;


    // -------------------- GETTERS --------------------

    public Long getTemplateId() {
        return templateId;
    }

    public Template getTemplate() {
        return template;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Map<MetadataBlock, List<DatasetFieldsByType>> getMdbForEdit() {
        return mdbForEdit;
    }

    public Map<DatasetFieldType, InputFieldRenderer> getInputRenderersByFieldType() {
        return inputRenderersByFieldType;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        switch(editMode) {
            case CLONE: return initForClone();
            case CREATE: return initForCreate();
            case EDIT: return initForEdit();
            default: throw new RuntimeException("On Template page without id or ownerid."); // improve error handling
        }
    }

    public String save() {
        mdbForEdit.values()
                .forEach(v -> v.forEach(datasetFieldType -> saveDatasetFieldsGUIOrder(datasetFieldType.getDatasetFields())));
        template.setDatasetFields(DatasetFieldUtil.flattenDatasetFieldsFromBlocks(mdbForEdit));

        Try<Template> saveResult = handleSave();
        if (saveResult.isFailure()) {
            logger.fine(() -> String.format("There was a problem with action [%s] on template: %s", editMode, saveResult.getCause().getMessage()));
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("template.save.fail"), "");
            return StringUtils.EMPTY;
        }

        return "/manage-templates.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";
    }


    // -------------------- PRIVATE --------------------

    private String initForClone() {
        Template source = templateDao.find(templateId);
        dataverse = source.getDataverse();

        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        template = templateService.copyTemplate(source);
        initFields();
        return StringUtils.EMPTY;
    }

    private String initForEdit() {
        template = templateDao.find(templateId);
        dataverse = template.getDataverse();

        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        initFields();
        return StringUtils.EMPTY;
    }

    private String initForCreate() {
        dataverse = dataverseDao.find(ownerId);

        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        template = new Template(dataverse);
        initFields();
        return StringUtils.EMPTY;
    }

    private void initFields() {
        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(
                template.getDatasetFields(), dataverse.getMetadataBlockRootDataverse());
        template.setDatasetFields(datasetFields);
        inputRenderersByFieldType = inputFieldRendererManager.obtainRenderersByType(datasetFields);
        mdbForEdit = datasetFieldsInitializer.groupAndUpdateFlagsForEdit(datasetFields, dataverse.getMetadataBlockRootDataverse());

        if (template.getTermsOfUseAndAccess() == null) {
            template.setTermsOfUseAndAccess(prepareTermsOfUseAndAccess(template));
        }
    }

    private TermsOfUseAndAccess prepareTermsOfUseAndAccess(Template template) {
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setTemplate(template);
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
        return terms;
    }

    private void saveDatasetFieldsGUIOrder(List<DatasetField> datasetFields) {
        for (int i = 0; i < datasetFields.size(); ++i) {
            datasetFields.get(i).setDisplayOrder(i);
        }
    }

    private Try<Template> handleSave() {
        switch (editMode) {
            case CREATE: return tryToSave(templateService::createTemplate, "template.create");
            case EDIT: return tryToSave(templateService::updateTemplate,"template.save");
            case CLONE: return tryToSave(templateService::mergeIntoDataverse, "template.clone");
            default: throw new IllegalStateException();
        }
    }

    private Try<Template> tryToSave(BiFunction<Dataverse, Template, Try<Template>> saveHandler, String successMessage) {
        return saveHandler.apply(dataverse, template)
                .onSuccess(op -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle(successMessage)));
    }

    // -------------------- SETTERS --------------------

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    // -------------------- INNER CLASSES --------------------

    @FacesConverter("templateEditModeConverter")
    public static class EditModeConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            return StringUtils.isNotBlank(value) ? EditMode.valueOf(value) : null;
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            return value != null ? value.toString() : StringUtils.EMPTY;
        }
    }
}
