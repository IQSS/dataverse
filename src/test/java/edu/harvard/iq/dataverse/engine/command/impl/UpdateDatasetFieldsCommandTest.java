package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsValidator;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateDatasetFieldsCommandTest {

    @Mock
    private UpdateDatasetVersionCommand updateDatasetVersionCommandStub;
    @Mock
    private DataverseEngine dataverseEngineMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private DataverseRequest dataverseRequestStub;
    @Mock
    private TermsOfUseAndAccess termsOfUseAndAccessStub;
    @Mock
    private CommandContext commandContextMock;
    @Mock
    private DatasetVersion datasetVersionMock;
    @Mock
    private DatasetFieldsValidator datasetFieldsValidatorMock;

    private UpdateDatasetFieldsCommand sut;

    @BeforeEach
    public void setUp() throws CommandException {
        MockitoAnnotations.openMocks(this);

        when(dataverseEngineMock.submit(updateDatasetVersionCommandStub)).thenReturn(datasetMock);
        when(commandContextMock.engine()).thenReturn(dataverseEngineMock);
        when(commandContextMock.datasetFieldsValidator()).thenReturn(datasetFieldsValidatorMock);
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessStub);
    }

    @Test
    public void execute_invalidFields_shouldThrowException() {
        List<DatasetField> testUpdatedFields = new ArrayList<>();
        sut = new UpdateDatasetFieldsCommand(datasetMock, testUpdatedFields, true, dataverseRequestStub);

        when(datasetFieldsValidatorMock.validateFields(testUpdatedFields, datasetVersionMock)).thenReturn("validation error");

        CommandException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(commandContextMock));

        assertEquals(BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError", List.of("validation error")),
                exception.getMessage());
    }

    @Test
    public void execute_withSingleDatasetField_replaceDataTrue_updatesField() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, false);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "updated");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("updated", result.getLatestVersion().getDatasetField(fieldType).getValue());
    }

    @Test
    public void execute_withSingleEmptyDatasetField_replaceDataTrue_updatesFieldToEmpty() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, false);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertTrue(result.getLatestVersion().getDatasetField(fieldType).isEmpty());
    }

    @Test
    public void execute_withSingleDatasetField_replaceDataFalse_doesNotUpdateField() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, false);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "updated");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, false, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("original", result.getLatestVersion().getDatasetField(fieldType).getValue());
    }

    @Test
    public void execute_withMultipleDatasetField_replaceDataFalse_updatesField() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, true);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "updated");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, false, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("original", result.getLatestVersion().getDatasetField(fieldType).getDatasetFieldValues().get(0).getValue());
        assertEquals("updated", result.getLatestVersion().getDatasetField(fieldType).getDatasetFieldValues().get(1).getValue());
        assertEquals(2, result.getLatestVersion().getDatasetField(fieldType).getDatasetFieldValues().size());
    }

    @Test
    public void execute_withMultipleDatasetField_replaceDataTrue_updatesField() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, true);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "updated");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("updated", result.getLatestVersion().getDatasetField(fieldType).getValue());
        assertEquals(1, result.getLatestVersion().getDatasetField(fieldType).getDatasetFieldValues().size());
    }

    @Test
    public void execute_withMultipleEmptyDatasetField_replaceDataTrue_updatesFieldToEmpty() throws CommandException {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, true);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleValueField(fieldType, "original");
        List<DatasetField> updatedFields = createUpdatedSingleValueFieldList(fieldType, "");

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertTrue(result.getLatestVersion().getDatasetField(fieldType).isEmpty());
    }

    @Test
    public void execute_withSingleControlledVocabularyDatasetField_replaceDataTrue_updatesField() throws CommandException {
        DatasetFieldType fieldType = createControlledVocabularyDatasetFieldType(false);
        List<ControlledVocabularyValue> controlledVocabularyValueList = setupControlledVocabularyValueList(fieldType);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleControlledVocabularyValueField(fieldType, controlledVocabularyValueList.get(0));
        List<DatasetField> updatedFields = createUpdatedSingleControlledVocabularyValueFieldList(fieldType, controlledVocabularyValueList.get(1));

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("law", result.getLatestVersion().getDatasetField(fieldType).getSingleControlledVocabularyValue().getStrValue());
    }

    @Test
    public void execute_withSingleControlledVocabularyDatasetField_replaceDataFalse_doesNotUpdateField() throws CommandException {
        DatasetFieldType fieldType = createControlledVocabularyDatasetFieldType(false);
        List<ControlledVocabularyValue> controlledVocabularyValueList = setupControlledVocabularyValueList(fieldType);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleControlledVocabularyValueField(fieldType, controlledVocabularyValueList.get(0));
        List<DatasetField> updatedFields = createUpdatedSingleControlledVocabularyValueFieldList(fieldType, controlledVocabularyValueList.get(1));

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, false, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals("mgmt", result.getLatestVersion().getDatasetField(fieldType).getSingleControlledVocabularyValue().getStrValue());
    }

    @Test
    public void execute_withMultipleControlledVocabularyDatasetField_replaceDataTrue_updatesField() throws CommandException {
        DatasetFieldType fieldType = createControlledVocabularyDatasetFieldType(true);
        List<ControlledVocabularyValue> controlledVocabularyValueList = setupControlledVocabularyValueList(fieldType);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleControlledVocabularyValueField(fieldType, controlledVocabularyValueList.get(0));
        List<DatasetField> updatedFields = createUpdatedSingleControlledVocabularyValueFieldList(fieldType, controlledVocabularyValueList.get(1));

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals(1, result.getLatestVersion().getDatasetField(fieldType).getControlledVocabularyValues().size());
        assertEquals("law", result.getLatestVersion().getDatasetField(fieldType).getSingleControlledVocabularyValue().getStrValue());
    }

    @Test
    public void execute_withMultipleControlledVocabularyDatasetField_replaceDataFalse_updatesField() throws CommandException {
        DatasetFieldType fieldType = createControlledVocabularyDatasetFieldType(true);
        List<ControlledVocabularyValue> controlledVocabularyValueList = setupControlledVocabularyValueList(fieldType);
        DatasetVersion datasetVersion = prepareDatasetVersionWithSingleControlledVocabularyValueField(fieldType, controlledVocabularyValueList.get(0));
        List<DatasetField> updatedFields = createUpdatedSingleControlledVocabularyValueFieldList(fieldType, controlledVocabularyValueList.get(1));

        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, false, dataverseRequestStub, updateDatasetVersionCommandStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersion)).thenReturn("");

        Dataset result = sut.execute(commandContextMock);
        verify(commandContextMock).engine();

        assertEquals(2, result.getLatestVersion().getDatasetField(fieldType).getControlledVocabularyValues().size());
        assertEquals("mgmt", result.getLatestVersion().getDatasetField(fieldType).getControlledVocabularyValues().get(0).getStrValue());
        assertEquals("law", result.getLatestVersion().getDatasetField(fieldType).getControlledVocabularyValues().get(1).getStrValue());
    }

    private DatasetVersion prepareDatasetVersionWithSingleValueField(DatasetFieldType fieldType, String originalValue) {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setTermsOfUseAndAccess(new TermsOfUseAndAccess());

        List<DatasetField> fields = new ArrayList<>();
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(fieldType);
        field.setSingleValue(originalValue);
        fields.add(field);
        datasetVersion.setDatasetFields(fields);

        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersion);
        when(datasetMock.getLatestVersion()).thenReturn(datasetVersion);

        return datasetVersion;
    }

    private List<DatasetField> createUpdatedSingleValueFieldList(DatasetFieldType fieldType, String updatedValue) {
        List<DatasetField> updatedFields = new ArrayList<>();
        DatasetField updatedField = new DatasetField();
        updatedField.setDatasetFieldType(fieldType);
        updatedField.setSingleValue(updatedValue);
        updatedFields.add(updatedField);
        return updatedFields;
    }

    private DatasetVersion prepareDatasetVersionWithSingleControlledVocabularyValueField(DatasetFieldType fieldType, ControlledVocabularyValue controlledVocabularyValue) {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setTermsOfUseAndAccess(new TermsOfUseAndAccess());

        List<DatasetField> fields = new ArrayList<>();
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(fieldType);
        field.setSingleControlledVocabularyValue(controlledVocabularyValue);
        fields.add(field);
        datasetVersion.setDatasetFields(fields);

        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersion);
        when(datasetMock.getLatestVersion()).thenReturn(datasetVersion);

        return datasetVersion;
    }

    private List<DatasetField> createUpdatedSingleControlledVocabularyValueFieldList(DatasetFieldType fieldType, ControlledVocabularyValue controlledVocabularyValue) {
        List<DatasetField> updatedFields = new ArrayList<>();
        DatasetField updatedField = new DatasetField();
        updatedField.setDatasetFieldType(fieldType);
        updatedField.setSingleControlledVocabularyValue(controlledVocabularyValue);
        updatedFields.add(updatedField);
        return updatedFields;
    }

    private List<ControlledVocabularyValue> setupControlledVocabularyValueList(DatasetFieldType fieldType) {
        ControlledVocabularyValue controlledVocabularyValue1 = new ControlledVocabularyValue(1L, "mgmt", fieldType);
        ControlledVocabularyValue controlledVocabularyValue2 = new ControlledVocabularyValue(2L, "law", fieldType);
        List<ControlledVocabularyValue> controlledVocabularyValueList = Arrays.asList(
                controlledVocabularyValue1,
                controlledVocabularyValue2
        );
        fieldType.setControlledVocabularyValues(controlledVocabularyValueList);
        return controlledVocabularyValueList;
    }

    private DatasetFieldType createControlledVocabularyDatasetFieldType(boolean allowMultiples) {
        DatasetFieldType fieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, allowMultiples);
        fieldType.setAllowControlledVocabulary(true);
        fieldType.setMetadataBlock(new MetadataBlock());
        return fieldType;
    }
}
