package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quickperf.junit5.QuickPerfTest;
import org.quickperf.sql.annotation.AnalyzeSql;
import org.quickperf.sql.annotation.ExpectMaxDelete;
import org.quickperf.sql.annotation.ExpectMaxInsert;
import org.quickperf.sql.annotation.ExpectMaxSelect;
import org.quickperf.sql.annotation.ExpectMaxUpdate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Proof of Concept: Database query counting and performance assertion using QuickPerf.
 * <p>
 * This test demonstrates how QuickPerf automates query counting and enforces performance
 * regression thresholds (e.g., detecting unexpected SELECT queries or N+1 regressions) directly in JUnit.
 */
@JpaPerformanceTest
@QuickPerfTest
class DatasetExportQuickPerfIT {

    static JpaEntityManagerService jpa;
    static Dataset sampleDataset;
    static final int FILE_COUNT = 5;

    @BeforeAll
    static void setUp() {
        jpa.start();

        DatasetRecipe sampleRecipe = DatasetRecipe.of(
            DatasetTypeRecipe.dataset(),
            VersionRecipe.of(
                FileRecipe.regular(FILE_COUNT)
            )
        );

        var fixture = DatasetFixtureBuilder.builder().recipe(sampleRecipe).build();

        jpa.inTransactionVoid(em -> em.persist(fixture.datasetType()));

        sampleDataset = fixture.dataset();
        jpa.inTransactionVoid(em -> {
            for (DataFile dataFile : fixture.dataFiles()) {
                em.persist(dataFile);
            }
            em.persist(sampleDataset);
        });
    }

    @Test
    @DisplayName("QuickPerf POC: Assert query limits, bind parameter safety, and zero mutations during metadata export")
    @ExpectMaxSelect(55)
    @org.quickperf.sql.annotation.ExpectUpdate(0)
    @org.quickperf.sql.annotation.ExpectInsert(0)
    @org.quickperf.sql.annotation.ExpectDelete(0)
    @org.quickperf.sql.annotation.DisableQueriesWithoutBindParameters
    @org.quickperf.sql.annotation.DisableLikeWithLeadingWildcard
    @AnalyzeSql
    void shouldExportDatasetWithinExpectedQueryLimits() {
        Long datasetVersionId = sampleDataset.getVersions().get(0).getId();

        String json = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB");

            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            var details = provider.getDatasetFileDetails();
            return details.toString();
        });

        assertNotNull(json);
    }

    @Test
    @DisplayName("QuickPerf POC: Demonstration of N+1 regression detection (Disabled by default to keep build green)")
    @org.junit.jupiter.api.Disabled("Demonstrates automatic CI failure on N+1 queries. Enable to view QuickPerf diagnostic report.")
    @org.quickperf.sql.annotation.DisableSameSelectTypesWithDifferentParamValues
    void demonstrateNPlusOneDetection() {
        Long datasetVersionId = sampleDataset.getVersions().get(0).getId();

        jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB");

            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            return provider.getDatasetFileDetails().toString();
        });
    }
}
