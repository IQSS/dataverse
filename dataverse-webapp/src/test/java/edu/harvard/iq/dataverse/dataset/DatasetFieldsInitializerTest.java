package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillAuthorField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillDepositorField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillKeywordField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillKeywordTermOnlyField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillTitle;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeAuthorFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeCitationMetadataBlock;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDateOfDepositFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDepositorFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeGeographicBoundingBoxFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeGeospatialMetadataBlock;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeKeywordFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeSocialScienceMetadataBlock;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeTitleFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeUnitOfAnalysisFieldType;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
public class DatasetFieldsInitializerTest {

    @InjectMocks
    private DatasetFieldsInitializer datasetFieldsInitializer;

    @Mock
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;

    // -------------------- TESTS --------------------

    @Test
    public void prepareDatasetFieldsForView() {
        // given
        MetadataBlock citationBlock = makeCitationMetadataBlock();

        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);

        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");
        fillAuthorField(authorField, 1, "Jane Doe", "Jane Aff");

        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(makeDepositorFieldType(citationBlock), null);
        fillDepositorField(depositorField, "John Depositor");

        DatasetField dateOfDepositField = DatasetField.createNewEmptyDatasetField(makeDateOfDepositFieldType(citationBlock), null);

        List<DatasetField> datasetFields = Lists.newArrayList(titleField, depositorField, authorField, dateOfDepositField);

        // when
        List<DatasetField> retDatasetFields = datasetFieldsInitializer.prepareDatasetFieldsForView(datasetFields);

        // then
        assertEquals(2, retDatasetFields.size());

        assertEquals(authorField.getDatasetFieldType(), retDatasetFields.get(0).getDatasetFieldType());
        assertEquals("John Doe; John Aff; Jane Doe; Jane Aff", retDatasetFields.get(0).getCompoundRawValue());

        assertEquals(depositorField.getDatasetFieldType(), retDatasetFields.get(1).getDatasetFieldType());
        assertEquals("John Depositor", retDatasetFields.get(1).getRawValue());
    }

    @Test
    public void prepareDatasetFieldsForEdit() {
        // given
        Dataverse metadataRootDataverse = new Dataverse();
        MetadataBlock citationBlock = makeCitationMetadataBlock();
        MetadataBlock socialScienceBlock = makeSocialScienceMetadataBlock();
        metadataRootDataverse.setMetadataBlocks(Lists.newArrayList(citationBlock));


        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        fillTitle(titleField, "Some Title");

        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");
        fillAuthorField(authorField, 1, "Jane Doe", "Jane Aff");

        DatasetField keywordField = DatasetField.createNewEmptyDatasetField(makeKeywordFieldType(citationBlock), null);
        fillKeywordField(keywordField, 0, "term1", "vocabName", "http://example.edu");
        fillKeywordTermOnlyField(keywordField, 1, "term2");
        fillKeywordTermOnlyField(keywordField, 2, "term3");

        DatasetFieldType depositorFieldType = makeDepositorFieldType(citationBlock);
        DatasetFieldType dataOfDepositFieldType = makeDateOfDepositFieldType(citationBlock);

        DatasetField unitOfAnalysisField = DatasetField.createNewEmptyDatasetField(makeUnitOfAnalysisFieldType(socialScienceBlock), null);

        List<DatasetField> datasetFields = Lists.newArrayList(keywordField, titleField, authorField, unitOfAnalysisField);

        // when
        List<DatasetField> retDatasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(datasetFields, metadataRootDataverse);

        // then
        assertEquals(5, retDatasetFields.size());

        assertEquals(titleField.getDatasetFieldType(), retDatasetFields.get(0).getDatasetFieldType());
        assertEquals("Some Title", retDatasetFields.get(0).getRawValue());

        assertEquals(authorField.getDatasetFieldType(), retDatasetFields.get(1).getDatasetFieldType());
        assertEquals("John Doe; John Aff; Jane Doe; Jane Aff", retDatasetFields.get(1).getCompoundRawValue());

        assertEquals(keywordField.getDatasetFieldType(), retDatasetFields.get(2).getDatasetFieldType());
        assertEquals("term1; vocabName; http://example.edu; term2; ; ; term3; ; ", retDatasetFields.get(2).getCompoundRawValue());

        assertEquals(depositorFieldType, retDatasetFields.get(3).getDatasetFieldType());
        assertEquals("", retDatasetFields.get(3).getRawValue());

        assertEquals(dataOfDepositFieldType, retDatasetFields.get(4).getDatasetFieldType());
        assertEquals("", retDatasetFields.get(4).getRawValue());
    }

    @Test
    public void groupAndUpdateEmptyAndRequiredFlag() {
        // given
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);

        MetadataBlock citationBlock = makeCitationMetadataBlock();
        MetadataBlock geospatialBlock = makeGeospatialMetadataBlock();
        MetadataBlock socialScienceBlock = makeSocialScienceMetadataBlock();

        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        fillTitle(titleField, "Some Title");

        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");

        DatasetField boundingBoxField = DatasetField.createNewEmptyDatasetField(makeGeographicBoundingBoxFieldType(geospatialBlock), null);

        DatasetField unitOfAnalysisField = DatasetField.createNewEmptyDatasetField(makeUnitOfAnalysisFieldType(socialScienceBlock), null);

        List<DatasetField> datasetFields = Lists.newArrayList(titleField, authorField, unitOfAnalysisField, boundingBoxField);
        datasetVersion.setDatasetFields(datasetFields);


        // when
        Map<MetadataBlock, List<DatasetField>> retMetadataBlocks = datasetFieldsInitializer.groupAndUpdateEmptyAndRequiredFlag(datasetFields);

        // then
        assertEquals(3, retMetadataBlocks.size());
        assertThat(retMetadataBlocks.keySet(), contains(citationBlock, geospatialBlock, socialScienceBlock));

        assertTrue(citationBlock.isHasRequired());
        assertFalse(citationBlock.isEmpty());
        assertEquals(2, retMetadataBlocks.get(citationBlock).size());

        assertFalse(geospatialBlock.isHasRequired());
        assertTrue(geospatialBlock.isEmpty());
        assertEquals(1, retMetadataBlocks.get(geospatialBlock).size());

        assertFalse(socialScienceBlock.isHasRequired());
        assertTrue(socialScienceBlock.isEmpty());
        assertEquals(1, retMetadataBlocks.get(socialScienceBlock).size());
    }

    @Test
    public void updateDatasetFieldIncludeFlag() {
        //given
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        DatasetVersion datasetVersion = new DatasetVersion();
        List<DatasetField> datasetFields = prepareTestData(dataset, datasetVersion);
        Optional<Long> titleField = retrieveTitleField(datasetFields);

        Mockito.when(dataverseFieldTypeInputLevelService.findByDataverseIdAndDatasetFieldTypeIdList(any(), any()))
                .thenReturn(prepareHiddenFields(Lists.newArrayList(titleField.get())));

        //when
        List<DatasetField> updatedDsf = datasetFieldsInitializer.updateDatasetFieldIncludeFlag(datasetFields, dataverse);

        Optional<DatasetField> titleDsf = updatedDsf.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals("title"))
                .findAny();

        //then
        Assert.assertEquals(3, updatedDsf.stream().filter(DatasetField::isInclude).count());
        Assert.assertTrue(titleDsf.isPresent());
        Assert.assertFalse(titleDsf.get().isInclude());
    }

    // -------------------- PRIVATE --------------------

    private Optional<Long> retrieveTitleField(List<DatasetField> datasetFields) {
        return datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals("title"))
                .map(datasetField -> datasetField.getDatasetFieldType().getId())
                .findAny();
    }

    private List<DatasetField> prepareTestData(Dataset dataset, DatasetVersion datasetVersion) {
        datasetVersion.setDataset(dataset);

        MetadataBlock citationBlock = makeCitationMetadataBlock();

        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        fillTitle(titleField, "Some Title");

        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");

        List<DatasetField> datasetFields = Lists.newArrayList(titleField, authorField);

        datasetVersion.setDatasetFields(datasetFields);
        return datasetFields;
    }

    private List<DataverseFieldTypeInputLevel> prepareHiddenFields(List<Long> listOfIdsToHide) {

        return listOfIdsToHide.stream()
                .map(id -> {
                    DataverseFieldTypeInputLevel dataverseField = new DataverseFieldTypeInputLevel();
                    DatasetFieldType fakeField = new DatasetFieldType();
                    fakeField.setId(id);
                    dataverseField.setDatasetFieldType(fakeField);
                    return dataverseField;
                })
                .collect(Collectors.toList());
    }
}
