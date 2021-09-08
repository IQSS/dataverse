package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.importer.metadata.ImporterConstants;
import edu.harvard.iq.dataverse.importer.metadata.ImporterData;
import edu.harvard.iq.dataverse.importer.metadata.ImporterFieldKey;
import edu.harvard.iq.dataverse.importer.metadata.ImporterFieldType;
import edu.harvard.iq.dataverse.importer.metadata.MetadataImporter;
import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.importer.metadata.SafeBundleWrapper;
import edu.harvard.iq.dataverse.importers.ui.form.FormConstants;
import edu.harvard.iq.dataverse.importers.ui.form.FormItem;
import edu.harvard.iq.dataverse.importers.ui.form.ItemType;
import edu.harvard.iq.dataverse.importers.ui.form.ProcessingType;
import edu.harvard.iq.dataverse.importers.ui.form.ResultGroup;
import edu.harvard.iq.dataverse.importers.ui.form.ResultGroupsCreator;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItemsCreator;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.component.fileupload.FileUpload;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ImporterForm {
    private static final Logger logger = LoggerFactory.getLogger(ImporterForm.class);

    private static final String METADATA_IMPORT_RESULT_ERROR = "metadata.import.result.error";
    private static final String UPLOAD_SUCCESSFUL = "metadata.import.upload.successful";

    public enum ImportStep {
        FIRST, SECOND;
    }

    private List<FormItem> items = Collections.emptyList();
    private List<ResultGroup> resultGroups = new ArrayList<>();
    private Map<ImporterFieldKey, FormItem> keyToItem = Collections.emptyMap();
    private ImportStep step;

    private MetadataImporter importer;
    private SafeBundleWrapper bundleWrapper;
    private MetadataFormLookup lookup;

    // -------------------- CONSTRUCTORS --------------------

    public ImporterForm() {
        this.step = ImportStep.FIRST;
    }


    // -------------------- GETTERS --------------------

    public List<FormItem> getItems() {
        return items;
    }

    public List<ResultGroup> getResultGroups() {
        return resultGroups;
    }

    public ImportStep getStep() {
        return step;
    }

    public ProcessingType[] getItemProcessingOptions(ResultItem item) {
        return ItemType.VOCABULARY.equals(item.getItemType())
                ? FormConstants.SINGLE_OPTIONS
                : item.getMultipleAllowed()
                    ? FormConstants.MULTIPLE_OPTIONS
                    : FormConstants.SINGLE_OPTIONS;
     }

    // -------------------- LOGIC --------------------

    public static ImporterForm createInitializedForm(MetadataImporter importer, Locale locale,
                                                     Supplier<Map<MetadataBlock, List<DatasetFieldsByType>>> metadataSupplier) {
        ImporterForm instance = new ImporterForm();
        instance.initializeForm(importer, locale,
                MetadataFormLookup.create(importer.getMetadataBlockName(), metadataSupplier));
        return instance;
    }

    public void initializeForm(MetadataImporter importer, Locale locale, MetadataFormLookup lookup) {
        this.lookup = lookup;
        this.importer = importer;
        this.bundleWrapper = SafeBundleWrapper.createFromImporter(importer, locale);
        Tuple2<List<FormItem>, Map<ImporterFieldKey, FormItem>> itemsAndKeyToItem = initializeFormItems();
        this.items = itemsAndKeyToItem._1;
        this.keyToItem = itemsAndKeyToItem._2;
    }

    public void handleFileUpload(FileUploadEvent event) throws IOException {
        FileUpload component = (FileUpload) event.getComponent();
        removePreviousTempFile(component);

        UploadedFile file = Optional.ofNullable(event)
                .map(FileUploadEvent::getFile)
                .orElseThrow(() -> new IllegalStateException("Null event or file"));
        Path tempPath = prepareTempPath(file);
        Files.copy(file.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);
        component.setValue(tempPath.toFile());
        FacesContext.getCurrentInstance().addMessage(component.getClientId(),
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                BundleUtil.getStringFromBundle(UPLOAD_SUCCESSFUL, file.getFileName()),
                StringUtils.EMPTY));
    }

    public void nextStep() {
        Set<ValidationResult> validationResults = validateFormInputs();
        if (!validationResults.isEmpty()) {
            showValidationMessages(validationResults);
            return;
        }
        try {
            List<ResultField> resultFields = importer.fetchMetadata(toImporterInput(items));
            resultGroups = new ResultGroupsCreator()
                    .createResultGroups(new ResultItemsCreator(lookup).createItemsForView(resultFields));
            step = ImportStep.SECOND;
        } catch (RuntimeException re) {
            logger.warn("Exception during importer invocation", re);
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle(METADATA_IMPORT_RESULT_ERROR), "");
        }
    }

    public void fillFormAndCleanUp(Map<MetadataBlock, List<DatasetFieldsByType>> metadata) {
        cleanUp();
        new MetadataFormFiller(lookup).fillForm(
                new ResultGroupsCreator().prepareForFormFill(resultGroups)
        );
    }

    public void handleClose(CloseEvent event) {
        cleanUp();
    }

    // -------------------- PRIVATE --------------------

    private Tuple2<List<FormItem>, Map<ImporterFieldKey, FormItem>> initializeFormItems() {
        List<FormItem> items = new ArrayList<>();
        Map<ImporterFieldKey, FormItem> keyToItem = new HashMap<>();
        int counter = 1;

        for (ImporterData.ImporterField field : getImporterFields(importer)) {
            FormItem formItem = new FormItem(generateViewId(field, counter), field, bundleWrapper);
            items.add(formItem);
            keyToItem.put(field.fieldKey, formItem);
            counter++;
        }
        return Tuple.of(items, keyToItem);
    }

    private String generateViewId(ImporterData.ImporterField field, int ordinal) {
        return String.join("_",
                String.valueOf(Math.abs(field.fieldKey.hashCode() % 512)),
                field.fieldKey.getName(),
                String.valueOf(ordinal));
    }

    private List<ImporterData.ImporterField> getImporterFields(MetadataImporter importer) {
        return Optional.ofNullable(importer)
                .map(MetadataImporter::getImporterData)
                .map(ImporterData::getImporterFormSchema)
                .orElseGet(Collections::emptyList);
    }

    private Path prepareTempPath(UploadedFile file) throws IOException {
        String tempDirectory = FileUtil.getFilesTempDirectory();
        return Files.createTempFile(Paths.get(tempDirectory), "import",
                ImporterConstants.FILE_NAME_SEPARATOR + file.getFileName());
    }

    private void removePreviousTempFile(FileUpload component) {
        if (component.getValue() == null) {
            return;
        }
        File tempFile = (File) component.getValue();
        tempFile.delete();
    }

    private Map<ImporterFieldKey, Object> toImporterInput(Collection<FormItem> formItems) {
        return formItems.stream()
                .filter(FormItem::isRelevantForProcessing)
                .collect(HashMap::new,
                        (m, i) -> m.put(i.getImporterField().fieldKey, i.getValue()),
                        HashMap::putAll);
    }

    private Set<ValidationResult> validateFormInputs() {
        ValidationInput input = prepareItemsForValidation();
        Map<ImporterFieldKey, String> validated = importer.validate(toImporterInput(input.itemsForImporterValidation));
        Set<ValidationResult> validationResults = input.emptyItems.stream()
                .map(i -> new ValidationResult(i.getViewId(), fetchEmptyInputMessage(i)))
                .collect(Collectors.toSet());
        return validated.entrySet().stream()
                .map(e -> new ValidationResult(keyToItem.get(e.getKey()).getViewId(), bundleWrapper.getString(e.getValue())))
                .collect(() -> validationResults, Set::add, Set::addAll);
    }

    private ValidationInput prepareItemsForValidation() {
        Set<FormItem> itemsForValidation = items.stream()
                .filter(FormItem::isRelevantForProcessing)
                .collect(Collectors.toSet());
        Set<FormItem> notFilled = itemsForValidation.stream()
                .filter(i -> i.getRequired() && i.getValue() == null)
                .collect(Collectors.toSet());
        itemsForValidation.removeAll(notFilled);
        return new ValidationInput(itemsForValidation, notFilled);
    }

    private String fetchEmptyInputMessage(FormItem item) {
        String bundleKey = ImporterFieldType.UPLOAD_TEMP_FILE.equals(item.getType())
                ? FormConstants.EMPTY_FILE_MESSAGE_KEY
                : FormConstants.EMPTY_FIELD_MESSAGE_KEY;
        return BundleUtil.getStringFromBundle(bundleKey);
    }

    private void showValidationMessages(Set<ValidationResult> validationResults) {
        FacesContext fctx = FacesContext.getCurrentInstance();
        JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("metadata.import.form.validation.error"), "");
        for (ValidationResult result : validationResults) {
            Optional<UIComponent> soughtComponent =
                    JsfHelper.findComponent(Optional.ofNullable(fctx.getViewRoot()), result.viewId, String::endsWith);
            if (!soughtComponent.isPresent()) {
                continue;
            }
            UIComponent component = soughtComponent.get();
            if (component instanceof UIInput) {
                ((UIInput) component).setValid(false);
            }
            fctx.addMessage(component.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, StringUtils.EMPTY, result.message));
        }
    }

    private void cleanUp() {
        items.stream()
                .filter(i -> i.getType().equals(ImporterFieldType.UPLOAD_TEMP_FILE) && i.getValue() != null)
                .forEach(i -> ((File) i.getValue()).delete());
    }

    // -------------------- INNER CLASSES --------------------

    private static class ValidationInput {
        public final Set<FormItem> itemsForImporterValidation;
        public final Set<FormItem> emptyItems;

        public ValidationInput(Set<FormItem> itemsForImporterValidation, Set<FormItem> emptyItems) {
            this.itemsForImporterValidation = itemsForImporterValidation;
            this.emptyItems = emptyItems;
        }
    }

    private static class ValidationResult {
        public final String viewId;
        public final String message;

        public ValidationResult(String viewId, String message) {
            this.viewId = viewId;
            this.message = message;
        }
    }
}
