package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.test.WithTestClock;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Arrays;

public final class DatasetMother implements WithTestClock {

    private DatasetMother() {}

    public static Dataset givenDataset(long id) {
        Dataset dataset = givenDataset();
        dataset.setId(id);
        return dataset;
    }

    public static Dataset givenDataset() {
        return givenDataset(clock);
    }

    public static Dataset givenDataset(Clock clock) {
        Dataset dataset = new Dataset();
        dataset.setCreateDate(new Timestamp(clock.millis()));
        dataset.setModificationTime(new Timestamp(clock.millis()));
        dataset.getVersions().get(0).setCreateTime(new Timestamp(clock.millis()));
        dataset.getVersions().get(0).setLastUpdateTime(new Timestamp(clock.millis()));
        return dataset;
    }

    public static DatasetVersion givenDatasetVersion(DatasetField... metadataFields) {
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(Arrays.asList(metadataFields));
        return version;
    }

    public static DatasetField createField(String typeName, String value) {
        DatasetField field = new DatasetField();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName(typeName);
        field.setDatasetFieldType(fieldType);
        field.setValue(value);
        return field;
    }

    public static DatasetField createField(String typeName, DatasetField... children) {
        DatasetField field = createField(typeName, (String) null);
        field.setDatasetFieldsChildren(Arrays.asList(children));
        return field;
    }
}
