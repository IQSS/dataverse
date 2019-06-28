package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.FieldType;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.search.dto.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.dto.NumberSearchField;
import edu.harvard.iq.dataverse.search.dto.SearchBlock;
import edu.harvard.iq.dataverse.search.dto.SearchField;
import edu.harvard.iq.dataverse.search.dto.TextSearchField;
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.vavr.Tuple;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * Page class responsible for showing search fields for Metadata blocks, files/dataverses blocks
 * and redirecting to search results.
 */
@ViewScoped
@Named("AdvancedSearchPage")
public class AdvancedSearchPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(AdvancedSearchPage.class.getCanonicalName());

    @EJB
    private DataverseServiceBean dataverseServiceBean;

    @EJB
    private DatasetFieldServiceBean datasetFieldService;

    @Inject
    private WidgetWrapper widgetWrapper;

    @Inject
    private SolrQueryCreator solrQueryCreator;

    private Dataverse dataverse;
    private String dataverseIdentifier;

    private SearchBlock dataversesSearchBlock;
    private SearchBlock filesSearchBlock;
    private List<SearchBlock> metadataSearchBlocks = new ArrayList<>();

    // -------------------- LOGIC --------------------

    /**
     * Initalizes all components required to view the the page correctly.
     */
    public void init() {

        if (dataverseIdentifier != null) {
            dataverse = dataverseServiceBean.findByAlias(dataverseIdentifier);
        }
        if (dataverse == null) {
            dataverse = dataverseServiceBean.findRootDataverse();
        }
        List<MetadataBlock> metadataBlocks = dataverse.getRootMetadataBlocks();
        List<DatasetFieldType> metadataFieldList = datasetFieldService.findAllAdvancedSearchFieldTypes();

        mapAllMetadataBlocks(metadataFieldList, metadataBlocks);

        mapDataversesAndFilesBlocks();
    }

    /**
     * Composes query and redirects to the page with results.
     *
     * @return url with query
     * @throws IOException
     */
    public String find() throws IOException {
        List<SearchBlock> allSearchBlocks = new ArrayList<>(metadataSearchBlocks);
        allSearchBlocks.add(filesSearchBlock);
        allSearchBlocks.add(dataversesSearchBlock);

        String query = solrQueryCreator.constructQuery(allSearchBlocks);

        String returnString = "/dataverse.xhtml?q=";
        returnString += URLEncoder.encode(query, "UTF-8");
        returnString += "&alias=" + dataverse.getAlias() + "&faces-redirect=true";
        returnString = widgetWrapper.wrapURL(returnString);

        logger.fine(returnString);
        return returnString;
    }

    // -------------------- PRIVATE --------------------

    private void mapDataversesAndFilesBlocks() {
        dataversesSearchBlock = new SearchBlock("dataverses",
                                                BundleUtil.getStringFromBundle("advanced.search.header.dataverses"), constructDataversesSearchFields());

        filesSearchBlock = new SearchBlock("files",
                                           BundleUtil.getStringFromBundle("advanced.search.header.files"),
                                           constructFilesSearchFields());
    }

    private void mapAllMetadataBlocks(List<DatasetFieldType> metadataFieldList, List<MetadataBlock> metadataBlocks) {
        for (MetadataBlock mdb : metadataBlocks) {

            List<DatasetFieldType> filteredDatasetFields = metadataFieldList.stream()
                    .filter(datasetFieldType -> datasetFieldType.getMetadataBlock().getId().equals(mdb.getId()))
                    .collect(toList());

            List<SearchField> searchFields = mapMetadataBlockFieldsToSearchFields(filteredDatasetFields, mdb);

            metadataSearchBlocks.add(new SearchBlock(mdb.getName(), mdb.getLocaleDisplayName(), searchFields));
        }
        addExtraFieldsToCitationMetadataBlock();
    }

    private List<SearchField> mapMetadataBlockFieldsToSearchFields(List<DatasetFieldType> metadataFieldList, MetadataBlock mdb) {
        return metadataFieldList.stream()
                .map(this::mapDatasetFields)
                .filter(searchField -> !searchField.getName().isEmpty())
                .collect(toList());
    }

    private void addExtraFieldsToCitationMetadataBlock() {
        metadataSearchBlocks.stream()
                .filter(searchBlock -> searchBlock.getBlockName().equals(SearchFields.DATASET_CITATION))
                .forEach(searchBlock -> {

                    searchBlock.addSearchField(new TextSearchField(SearchFields.DATASET_PERSISTENT_ID,
                                                                   BundleUtil.getStringFromBundle("dataset.metadata.persistentId"),
                                                                   BundleUtil.getStringFromBundle("dataset.metadata.persistentId.tip")));

                    searchBlock.addSearchField(new TextSearchField(SearchFields.DATASET_PUBLICATION_DATE,
                                                                   BundleUtil.getStringFromBundle("dataset.metadata.publicationYear"),
                                                                   BundleUtil.getStringFromBundle("dataset.metadata.publicationYear.tip")));

                });
    }

    private SearchField mapDatasetFields(DatasetFieldType datasetFieldType) {

        if (containsCheckboxValues(datasetFieldType)) {

            return mapCheckBoxValues(datasetFieldType);

        } else if (isTextOrDateField(datasetFieldType)) {
            return new TextSearchField(datasetFieldType.getName(),
                                       datasetFieldType.getDisplayName(),
                                       datasetFieldType.getLocaleDescription());

        } else if (isNumberField(datasetFieldType)) {
            return new NumberSearchField(datasetFieldType.getName(),
                                         datasetFieldType.getDisplayName(),
                                         datasetFieldType.getLocaleDescription());
        }

        return new TextSearchField(StringUtils.EMPTY,
                                   StringUtils.EMPTY,
                                   StringUtils.EMPTY);
    }

    private CheckboxSearchField mapCheckBoxValues(DatasetFieldType datasetFieldType) {
        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(datasetFieldType.getName(),
                                                                          datasetFieldType.getDisplayName(),
                                                                          datasetFieldType.getLocaleDescription());

        for (ControlledVocabularyValue vocabValue : datasetFieldType.getControlledVocabularyValues()) {
            checkboxSearchField.getCheckboxLabelAndValue().add(Tuple.of(vocabValue.getLocaleStrValue(),
                                                                        vocabValue.getStrValue()));

        }
        return checkboxSearchField;
    }

    private boolean containsCheckboxValues(DatasetFieldType datasetFieldType) {
        return !datasetFieldType.getControlledVocabularyValues().isEmpty();
    }

    private boolean isNumberField(DatasetFieldType datasetFieldType) {
        return datasetFieldType.getFieldType().equals(FieldType.INT) ||
                datasetFieldType.getFieldType().equals(FieldType.FLOAT);
    }

    private boolean isTextOrDateField(DatasetFieldType datasetFieldType) {
        return datasetFieldType.getFieldType().equals(FieldType.TEXT) ||
                datasetFieldType.getFieldType().equals(FieldType.TEXTBOX) ||
                datasetFieldType.getFieldType().equals(FieldType.DATE);
    }

    private List<SearchField> constructFilesSearchFields() {
        List<SearchField> filesSearchFields = new ArrayList<>();

        filesSearchFields.add(new TextSearchField(SearchFields.FILE_NAME,
                                                  BundleUtil.getStringFromBundle("name"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.name.tip")));

        filesSearchFields.add(new TextSearchField(SearchFields.FILE_DESCRIPTION,
                                                  BundleUtil.getStringFromBundle("description"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.description.tip")));

        filesSearchFields.add(new TextSearchField(SearchFields.FILE_TYPE_SEARCHABLE,
                                                  BundleUtil.getStringFromBundle("advanced.search.files.fileType"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.fileType.tip")));

        filesSearchFields.add(new TextSearchField(SearchFields.FILE_PERSISTENT_ID,
                                                  BundleUtil.getStringFromBundle("advanced.search.files.persistentId"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.persistentId.tip")));

        filesSearchFields.add(new TextSearchField(SearchFields.VARIABLE_NAME,
                                                  BundleUtil.getStringFromBundle("advanced.search.files.variableName"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.variableName.tip")));

        filesSearchFields.add(new TextSearchField(SearchFields.VARIABLE_LABEL,
                                                  BundleUtil.getStringFromBundle("advanced.search.files.variableLabel"),
                                                  BundleUtil.getStringFromBundle("advanced.search.files.variableLabel.tip")));

        return filesSearchFields;
    }

    private List<SearchField> constructDataversesSearchFields() {
        List<SearchField> dataversesSearchFields = new ArrayList<>();

        dataversesSearchFields.add(new TextSearchField(SearchFields.DATAVERSE_NAME,
                                                       BundleUtil.getStringFromBundle("name"),
                                                       BundleUtil.getStringFromBundle("advanced.search.dataverses.name.tip")));

        dataversesSearchFields.add(new TextSearchField(SearchFields.DATAVERSE_ALIAS,
                                                       BundleUtil.getStringFromBundle("identifier"),
                                                       BundleUtil.getStringFromBundle("dataverse.identifier.title")));

        dataversesSearchFields.add(new TextSearchField(SearchFields.DATAVERSE_AFFILIATION,
                                                       BundleUtil.getStringFromBundle("affiliation"),
                                                       BundleUtil.getStringFromBundle("advanced.search.dataverses.affiliation.tip")));

        dataversesSearchFields.add(new TextSearchField(SearchFields.DATAVERSE_DESCRIPTION,
                                                       BundleUtil.getStringFromBundle("description"),
                                                       BundleUtil.getStringFromBundle("advanced.search.dataverses.description.tip")));

        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(SearchFields.DATAVERSE_SUBJECT,
                                                                          BundleUtil.getStringFromBundle("subject"),
                                                                          BundleUtil.getStringFromBundle("advanced.search.dataverses.subject.tip"));

        DatasetFieldType subjectType = datasetFieldService.findByName(DatasetFieldConstant.subject);

        for (ControlledVocabularyValue vocabValue : subjectType.getControlledVocabularyValues()) {
            checkboxSearchField.getCheckboxLabelAndValue().add(Tuple.of(vocabValue.getLocaleStrValue(),
                                                                        vocabValue.getStrValue()));

        }

        dataversesSearchFields.add(checkboxSearchField);

        return dataversesSearchFields;
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
