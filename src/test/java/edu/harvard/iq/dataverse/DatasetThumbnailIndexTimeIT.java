package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The API loads the dataset before the thumbnail command runs, and the thumbnail setters of
 * {@link DatasetServiceBean} merge that whole copy. When the background job of an earlier change records its
 * index time in between, the merge restores the older index time: the dataset then looks stale although its
 * index is up to date, and nothing records a newer time, because changing the thumbnail does not reindex.
 */
@JpaPerformanceTest
@Disabled("Known bug: setting a thumbnail can restore an older index time. Enable once it is fixed.")
class DatasetThumbnailIndexTimeIT {

    static JpaEntityManagerService jpa;

    @Test
    void settingTheThumbnailKeepsTheIndexTime() {
        jpa.start();
        var fixture = DatasetFixtureBuilder.builder()
            .recipe(DatasetRecipe.of(DatasetTypeRecipe.dataset(), VersionRecipe.of(FileRecipe.regular(1)))).build();
        DatasetType datasetType = fixture.datasetType();
        jpa.inTransactionVoid(em -> em.persist(datasetType));
        Dataset dataset = fixture.dataset();
        DataFile dataFile = fixture.dataFiles().get(0);
        jpa.inTransactionVoid(em -> {
            // DataFile has no cascade path from Dataset
            dataFile.setOwner(dataset);
            em.persist(dataFile);
            em.persist(dataset);
        });
        Long datasetId = dataset.getId();
        Timestamp oldIndexTime = Timestamp.valueOf("2026-01-01 10:00:00");
        jpa.inTransactionVoid(em -> em.find(Dataset.class, datasetId).setIndexTime(oldIndexTime));

        // the API loads the dataset for the thumbnail command (findDatasetOrDie)
        Dataset loadedForTheCommand = jpa.inTransaction(em -> em.find(Dataset.class, datasetId));

        // meanwhile the background job of an upload records its index time (IndexServiceBean.updateLastIndexedTimeInNewTransaction)
        Timestamp newIndexTime = Timestamp.valueOf("2026-01-01 10:00:02");
        jpa.inTransactionVoid(em -> {
            DvObject dvObject = em.find(DvObject.class, datasetId);
            dvObject.setIndexTime(newIndexTime);
            em.merge(dvObject);
        });

        // the thumbnail command runs
        jpa.inTransactionVoid(em -> {
            DatasetServiceBean datasetService = new DatasetServiceBean();
            datasetService.em = em;
            datasetService.setDatasetFileAsThumbnail(loadedForTheCommand, dataFile);
        });

        Timestamp indexTime = jpa.inTransaction(em -> em.find(Dataset.class, datasetId).getIndexTime());
        assertEquals(newIndexTime, indexTime, "setting the thumbnail restored the index time the dataset had when it was loaded");
    }
}
