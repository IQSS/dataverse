package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DatasetVersionValidationTest {

    @Test
    void requiredControlledVocabularyWithEmptyValueRemainsInvalid() {
        DatasetFieldType subjectType = new DatasetFieldType();
        subjectType.setName("subject");
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setRequired(true);
        subjectType.setAllowMultiples(false);
        subjectType.setFieldType(DatasetFieldType.FieldType.TEXT);
        subjectType.setControlledVocabularyValues(new ArrayList<>());
        subjectType.setChildDatasetFieldTypes(new ArrayList<>());

        MetadataBlock citation = new MetadataBlock();
        citation.setName("citation");
        citation.setDisplayName("Citation");
        citation.setDatasetFieldTypes(List.of(subjectType));
        subjectType.setMetadataBlock(citation);

        Dataverse dataverse = new Dataverse();
        dataverse.setMetadataBlockRoot(true);
        dataverse.setMetadataBlocks(List.of(citation));

        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);

        DatasetVersion version = new DatasetVersion();
        version.setVersionState(DatasetVersion.VersionState.DRAFT);
        version.setDataset(dataset);
        dataset.getVersions().add(version);

        DatasetField subjectField = new DatasetField();
        subjectField.setDatasetFieldType(subjectType);
        subjectField.setDatasetFieldValues(new ArrayList<>());
        subjectField.setControlledVocabularyValues(new ArrayList<>());

        DatasetFieldValue orphanValue = new DatasetFieldValue(subjectField);
        orphanValue.setValue("__placeholder__");
        subjectField.getDatasetFieldValues().add(orphanValue);

        version.setDatasetFields(new ArrayList<>(List.of(subjectField)));

        assertFalse(version.isValid(),
                "Controlled vocabulary fields without a selected term should keep dataset version invalid");
    }
}
