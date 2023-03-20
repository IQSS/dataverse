package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.advanced.QueryWrapperCreator;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldFactory;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GroupingSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.LicenseCheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.validation.SearchFormValidationService;
import edu.harvard.iq.dataverse.validation.ValidationEnhancer;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import edu.harvard.iq.dataverse.validation.field.validators.DateRangeValidator;
import io.vavr.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Page class responsible for showing search fields for Metadata blocks, files/dataverses blocks
 * and redirecting to search results.
 */
@ViewScoped
@Named("AdvancedSearchPage")
public class AdvancedSearchPage implements Serializable {

    private static final Logger logger = Logger.getLogger(AdvancedSearchPage.class.getCanonicalName());

    private DataverseDao dataverseDao;
    private DatasetFieldServiceBean datasetFieldService;
    private WidgetWrapper widgetWrapper;
    private QueryWrapperCreator queryWrapperCreator;
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    private SearchFormValidationService validationService;
    private SearchFieldFactory searchFieldFactory;
    private LicenseRepository licenseRepository;

    private Dataverse dataverse;
    private String dataverseIdentifier;

    private SearchBlock dataversesSearchBlock;
    private SearchBlock filesSearchBlock;
    private List<SearchBlock> metadataSearchBlocks = new ArrayList<>();

    private Map<String, SearchField> searchFieldIndex = new HashMap<>();
    private Map<String, SearchField> nonSearchFieldIndex = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public AdvancedSearchPage() { }

    @Inject
    public AdvancedSearchPage(DataverseDao dataverseDao, DatasetFieldServiceBean datasetFieldService,
                              WidgetWrapper widgetWrapper, QueryWrapperCreator queryWrapperCreator,
                              TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory, SearchFormValidationService validationService,
                              SearchFieldFactory searchFieldFactory, LicenseRepository licenseRepository) {
        this.dataverseDao = dataverseDao;
        this.datasetFieldService = datasetFieldService;
        this.widgetWrapper = widgetWrapper;
        this.queryWrapperCreator = queryWrapperCreator;
        this.termsOfUseSelectItemsFactory = termsOfUseSelectItemsFactory;
        this.validationService = validationService;
        this.searchFieldFactory = searchFieldFactory;
        this.licenseRepository = licenseRepository;
    }

    // -------------------- LOGIC --------------------

    public void init() {
        if (dataverseIdentifier != null) {
            dataverse = dataverseDao.findByAlias(dataverseIdentifier);
        }
        if (dataverse == null) {
            dataverse = dataverseDao.findRootDataverse();
        }
        buildFieldStructure();
    }

    /** Composes query and redirects to the page with results. */
    public String find() throws IOException {
        List<ValidationResult> validationResults
                = validationService.validateSearchForm(searchFieldIndex, nonSearchFieldIndex);
        if (!validationResults.isEmpty()) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("advanced.search.validation"), StringUtils.EMPTY);
            return StringUtils.EMPTY;
        }

        List<SearchBlock> allSearchBlocks = new ArrayList<>(metadataSearchBlocks);
        allSearchBlocks.add(filesSearchBlock);
        allSearchBlocks.add(dataversesSearchBlock);

        String returnString = buildSearchUrl(queryWrapperCreator.constructQueryWrapper(allSearchBlocks));
        logger.fine(returnString);
        return returnString;
    }

    // -------------------- PRIVATE --------------------

    private void buildFieldStructure() {
        extractSearchableFieldsForMetadataBlocks();
        createParentFieldsForSearchFields();
        createDataversesAndFilesBlocks();
    }

    private void extractSearchableFieldsForMetadataBlocks() {
        List<MetadataBlock> metadataBlocks = dataverse.getRootMetadataBlocks();
        List<Long> metadataBlockIds = metadataBlocks.stream()
                .map(MetadataBlock::getId)
                .collect(toList());
        Map<Long, List<DatasetFieldType>> metadataFieldListByBlock
                = datasetFieldService.findAllAdvancedSearchFieldTypesByMetadataBlockIds(metadataBlockIds).stream()
                .collect(Collectors.groupingBy(f -> f.getMetadataBlock().getId()));
        for (MetadataBlock block : metadataBlocks) {
            List<SearchField> searchFields
                    = metadataFieldListByBlock.getOrDefault(block.getId(), Collections.emptyList()).stream()
                    .map(searchFieldFactory::create)
                    .filter(f -> !SearchField.EMPTY.equals(f))
                    .collect(toList());
            searchFieldIndex.putAll(searchFields.stream()
                    .collect(Collectors.toMap(SearchField::getName, f -> f, (prev, next) -> next)));
            metadataSearchBlocks.add(new SearchBlock(block.getName(), block.getLocaleDisplayName(), searchFields));
        }
        addExtraFieldsToCitationMetadataBlock();
    }

    /**
     * As some validators are navigating through parent to access its other
     * subfields, we have to reproduce this structure. We're doing it by
     * accessing {@link DatasetFieldType} and creating new or retrieving
     * existing parent fields in order to connect them with search fields.
     */
    private void createParentFieldsForSearchFields() {
        int i = 0;
        for (SearchField field : searchFieldIndex.values()) {
            DatasetFieldType fieldType = field.getDatasetFieldType();
            if (fieldType == null || fieldType.getParentDatasetFieldType() == null) {
                continue;
            }
            DatasetFieldType parentType = fieldType.getParentDatasetFieldType();
            String parentKey = parentType.getName();
            SearchField parentField = searchFieldIndex.get(parentKey);
            parentField = parentField == null ? nonSearchFieldIndex.get(parentKey) : parentField;
            if (parentField == null) {
                parentField = new GroupingSearchField(parentKey, parentType.getDisplayName(), parentType.getDescription(),
                        null, parentType);
                parentField.setDisplayId(parentKey + "_" + (i++));
                nonSearchFieldIndex.put(parentKey, parentField);
            }
            parentField.getChildren().add(field);
            field.setParent(parentField);
        }
    }

    private void createDataversesAndFilesBlocks() {
        dataversesSearchBlock = new SearchBlock("dataverses",
                BundleUtil.getStringFromBundle("advanced.search.header.dataverses"), constructDataversesSearchFields());
        filesSearchBlock = new SearchBlock("files",
                BundleUtil.getStringFromBundle("advanced.search.header.files"), constructFilesSearchFields());
    }

    private void addExtraFieldsToCitationMetadataBlock() {
        for (SearchBlock b : metadataSearchBlocks) {
            if (SearchFields.DATASET_CITATION.equals(b.getBlockName())) {
                ValidationEnhancer enhancer = new ValidationEnhancer();
                TextSearchField persistentIdField = textFieldFromBundle(SearchFields.DATASET_PERSISTENT_ID, "dataset.metadata.persistentId", "dataset.metadata.persistentId.tip");
                DatasetFieldType publicationDateType = enhancer.createDatasetFieldType(SearchFields.DATASET_PUBLICATION_DATE,
                        BundleUtil.getStringFromBundle("dataset.metadata.publicationYear"),
                        BundleUtil.getStringFromBundle("dataset.metadata.publicationYear.tip"),
                        enhancer.createValidation(new DateRangeValidator(),
                                Collections.singletonMap(ValidationDescriptor.CONTEXT_PARAM, Collections.singletonList(ValidationDescriptor.SEARCH_CONTEXT))));
                DateSearchField publicationDateField = new DateSearchField(publicationDateType);
                b.addSearchField(persistentIdField);
                b.addSearchField(publicationDateField);
                searchFieldIndex.put(persistentIdField.getName(), persistentIdField);
                searchFieldIndex.put(publicationDateField.getName(), publicationDateField);
                break;
            }
        }
    }

    private List<SearchField> constructFilesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(SearchFields.FILE_NAME, "name", "advanced.search.files.name.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_DESCRIPTION, "description", "advanced.search.files.description.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_EXTENSION, "advanced.search.files.fileExtension", "advanced.search.files.fileExtension.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_PERSISTENT_ID, "advanced.search.files.persistentId", "advanced.search.files.persistentId.tip"));
        fields.add(textFieldFromBundle(SearchFields.VARIABLE_NAME, "advanced.search.files.variableName", "advanced.search.files.variableName.tip"));
        fields.add(textFieldFromBundle(SearchFields.VARIABLE_LABEL, "advanced.search.files.variableLabel", "advanced.search.files.variableLabel.tip"));

        Map<Long, String> licenseNames = licenseRepository.findAll().stream()
                .collect(Collectors.toMap(License::getId, License::getName, (prev, next) -> next));
        CheckboxSearchField licenseSearchField = new LicenseCheckboxSearchField(SearchFields.LICENSE,
                BundleUtil.getStringFromBundle("advanced.search.files.license"),
                BundleUtil.getStringFromBundle("advanced.search.files.license.tip"), licenseNames);

        for (SelectItem selectItem : termsOfUseSelectItemsFactory.buildLicenseSelectItems()) {
            licenseSearchField.getCheckboxLabelAndValue().add(Tuple.of(selectItem.getLabel(), selectItem.getValue().toString()));
        }
        fields.add(licenseSearchField);
        return fields;
    }

    private List<SearchField> constructDataversesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_NAME, "name", "advanced.search.dataverses.name.tip"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_ALIAS, "identifier","dataverse.identifier.title"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_AFFILIATION, "affiliation", "advanced.search.dataverses.affiliation.tip"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_DESCRIPTION, "description", "advanced.search.dataverses.description.tip"));

        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(SearchFields.DATAVERSE_SUBJECT,
                BundleUtil.getStringFromBundle("subject"),
                BundleUtil.getStringFromBundle("advanced.search.dataverses.subject.tip"));
        datasetFieldService.findByName(DatasetFieldConstant.subject)
                .getControlledVocabularyValues()
                .forEach(v -> checkboxSearchField.getCheckboxLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        fields.add(checkboxSearchField);

        return fields;
    }

    private TextSearchField textFieldFromBundle(String name, String displayNameKey, String descriptionKey) {
        return new TextSearchField(name, BundleUtil.getStringFromBundle(displayNameKey), BundleUtil.getStringFromBundle(descriptionKey));
    }

    private String buildSearchUrl(QueryWrapper queryWrapper) {
        List<String> filters = queryWrapper.getFilters();
        String filtersPart = IntStream.range(0, filters.size())
                .mapToObj(i -> "&fq" + i + "=" + safeEncode(filters.get(i)))
                .collect(Collectors.joining());

        return widgetWrapper.wrapURL(String.format("/dataverse.xhtml?q=%s&alias=%s",
                safeEncode(queryWrapper.getQuery()), dataverse.getAlias())
                + (StringUtils.isNotBlank(filtersPart) ? filtersPart : StringUtils.EMPTY)
                + "&faces-redirect=true");
    }

    private String safeEncode(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.WARNING, "Encoding problem: ", uee);
            throw new RuntimeException(uee);
        }
    }

    // -------------------- GETTERS --------------------

    public Dataverse getDataverse() {
        return dataverse;
    }

    public String getDataverseIdentifier() {
        return dataverseIdentifier;
    }

    public List<SearchBlock> getMetadataSearchBlocks() {
        return metadataSearchBlocks;
    }

    public SearchBlock getDataversesSearchBlock() {
        return dataversesSearchBlock;
    }

    public SearchBlock getFilesSearchBlock() {
        return filesSearchBlock;
    }

    // -------------------- SETTERS --------------------

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseIdentifier(String dataverseIdentifier) {
        this.dataverseIdentifier = dataverseIdentifier;
    }
}
