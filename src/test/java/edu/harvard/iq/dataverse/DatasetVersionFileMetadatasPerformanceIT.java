package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loading the files of a dataset version must not run queries per file: for datasets with tens of
 * thousands of files that takes many seconds (see #12739).
 */
@JpaPerformanceTest
class DatasetVersionFileMetadatasPerformanceIT {

    static final int FILES = 2000;

    static JpaEntityManagerService jpa;

    static Dataset dataset;

    @BeforeAll
    static void setUp() {
        jpa.start();
        var fixture = DatasetFixtureBuilder.builder()
            .recipe(DatasetRecipe.of(DatasetTypeRecipe.dataset(), VersionRecipe.of(FileRecipe.regular(FILES))))
            .build();
        jpa.inTransactionVoid(em -> em.persist(fixture.datasetType()));
        dataset = fixture.dataset();
        jpa.inTransactionVoid(em -> {
            for (DataFile dataFile : fixture.dataFiles()) {
                em.persist(dataFile);
            }
            em.persist(dataset);
        });
    }

    @Test
    void loadingFileMetadatasShouldNotQueryPerFile() {
        Long versionId = dataset.getVersions().get(0).getId();

        QueryCountHolder.clear();
        Instant start = Instant.now();
        // The version as a page gets it: its files are not loaded yet
        boolean hasRestrictedFile = jpa.inTransaction(em -> em.find(DatasetVersion.class, versionId).isHasRestrictedFile());
        long elapsed = start.until(Instant.now(), ChronoUnit.MILLIS);

        QueryCount count = QueryCountHolder.getGrandTotal();
        System.out.println("Files: " + FILES + ", elapsed ms: " + elapsed + ", select queries: " + count.getSelect());
        assertFalse(hasRestrictedFile);
        assertTrue(count.getSelect() < 100, "expected a few queries, not some per file, got " + count.getSelect());
    }
}
