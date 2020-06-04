package edu.harvard.iq.dataverse.persistence.dataset;

import java.sql.Timestamp;
import java.time.Clock;

public final class DatasetMother {

    private DatasetMother() {}

    public static Dataset givenDataset(Clock clock) {
        Dataset dataset = new Dataset();
        dataset.setCreateDate(new Timestamp(clock.millis()));
        dataset.setModificationTime(new Timestamp(clock.millis()));
        dataset.getVersions().get(0).setCreateTime(new Timestamp(clock.millis()));
        dataset.getVersions().get(0).setLastUpdateTime(new Timestamp(clock.millis()));
        return dataset;
    }
}
