package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.test.WithTestClock;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.function.Consumer;

public final class DatasetMother implements WithTestClock {

    private DatasetMother() {}

    public static Dataset givenDataset(long id) {
        Dataset dataset = givenDataset();
        dataset.setId(id);
        return dataset;
    }

    public static Dataset givenDataset() {
        Dataset dataset = new Dataset();
        dataset.setCreateDate(new Timestamp(clock.millis()));
        dataset.setModificationTime(new Timestamp(clock.millis()));
        dataset.getLatestVersion().setCreateTime(new Timestamp(clock.millis()));
        dataset.getLatestVersion().setLastUpdateTime(new Timestamp(clock.millis()));
        return dataset;
    }

    public static Dataset givenDataset(Consumer<Dataset> settings) {
        Dataset dataset = givenDataset();
        settings.accept(dataset);
        return dataset;
    }

    public static DatasetField givenDatasetFiled(String typeName, String value) {
        DatasetField field = new DatasetField();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName(typeName);
        field.setDatasetFieldType(fieldType);
        field.setValue(value);
        return field;
    }

    public static DatasetField givenDatasetFiled(String typeName, DatasetField... children) {
        DatasetField field = givenDatasetFiled(typeName, (String) null);
        field.setDatasetFieldsChildren(Arrays.asList(children));
        return field;
    }
}
