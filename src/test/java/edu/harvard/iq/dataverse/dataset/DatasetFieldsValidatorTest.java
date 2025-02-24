package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DatasetFieldsValidatorTest {

    @Mock
    private DatasetFieldServiceBean datasetFieldServiceMock;

    private DatasetFieldsValidator sut;

    @Mock
    private DatasetVersion datasetVersionMock;

    @Mock
    private Dataverse dataverseStub;

    @BeforeEach
    public void setup() {
        sut = new DatasetFieldsValidator();
        sut.datasetFieldService = datasetFieldServiceMock;
        setupDatasetVersionMock();
    }

    private void setupDatasetVersionMock() {
        Dataset datasetMock = Mockito.mock(Dataset.class);
        dataverseStub = Mockito.mock(Dataverse.class);
        Mockito.when(datasetMock.getOwner()).thenReturn(dataverseStub);
        Mockito.when(datasetVersionMock.getDataset()).thenReturn(datasetMock);
    }

    @Test
    void testValidateFields_emptyRequiredValuesOfAllTypes() {
        List<DatasetField> datasetFields = List.of(
                createEmptyRequiredMultipleDatasetField("testMultiple"),
                createEmptyControlledVocabularyDatasetField("testControlledVocabulary"),
                createEmptyCompoundDatasetField("testCompoundField"),
                createEmptySingleField("testSingleField")
        );

        String actualErrors = sut.validateFields(datasetFields, datasetVersionMock);

        assertTrue(actualErrors.contains(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredMultipleValueForField", List.of("testMultiple"))));
        assertTrue(actualErrors.contains(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredControlledVocabularyValueForField", List.of("testControlledVocabulary"))));
        assertTrue(actualErrors.contains(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredCompoundValueForField", List.of("testCompoundField"))));
        assertTrue(actualErrors.contains(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredSingleValueForField", List.of("testSingleField"))));
    }

    private DatasetField createEmptyControlledVocabularyDatasetField(String fieldName) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, false);
        datasetFieldType.setControlledVocabularyValues(List.of(
                new ControlledVocabularyValue(1L, "mgmt", datasetFieldType),
                new ControlledVocabularyValue(2L, "law", datasetFieldType),
                new ControlledVocabularyValue(3L, "cs", datasetFieldType)
        ));
        datasetFieldType.setAllowControlledVocabulary(true);

        DatasetField emptyField = new DatasetField();
        emptyField.setDatasetFieldType(datasetFieldType);
        emptyField.setSingleControlledVocabularyValue(new ControlledVocabularyValue(1L, "", datasetFieldType));

        Mockito.when(sut.datasetFieldService.isFieldRequiredInDataverse(datasetFieldType, dataverseStub)).thenReturn(true);
        return emptyField;
    }

    private DatasetField createEmptyRequiredMultipleDatasetField(String fieldName) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, true);

        DatasetField emptyField = new DatasetField();
        emptyField.setDatasetFieldType(datasetFieldType);
        emptyField.setDatasetFieldValues(Collections.emptyList());

        Mockito.when(sut.datasetFieldService.isFieldRequiredInDataverse(datasetFieldType, dataverseStub)).thenReturn(true);
        return emptyField;
    }

    private DatasetField createEmptyCompoundDatasetField(String fieldName) {
        DatasetFieldType compoundType = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, false);

        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(new DatasetFieldType("lat", DatasetFieldType.FieldType.TEXT, false));
        childTypes.add(new DatasetFieldType("lon", DatasetFieldType.FieldType.TEXT, false));

        childTypes.forEach(type -> type.setParentDatasetFieldType(compoundType));
        compoundType.setChildDatasetFieldTypes(childTypes);

        DatasetField emptyField = new DatasetField();
        emptyField.setDatasetFieldType(compoundType);
        return emptyField;
    }

    private DatasetField createEmptySingleField(String fieldName) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, false);

        DatasetField emptyField = new DatasetField();
        emptyField.setDatasetFieldType(datasetFieldType);
        emptyField.setSingleValue("");
        return emptyField;
    }
}
