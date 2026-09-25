package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
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
import org.quickperf.sql.annotation.ExpectDelete;
import org.quickperf.sql.annotation.ExpectInsert;
import org.quickperf.sql.annotation.ExpectMaxSelect;
import org.quickperf.sql.annotation.ExpectUpdate;

/**
 * Template: default read budget for data-service DB access.
 *
 * <p>Contract: a read of one dataset version's files must cost <b>at most
 * 3 SELECTs</b>. The fixture seeds {@link #FILE_COUNT} = 4 files, so an N+1
 * implementation fires 1 (version) + 1 (fileMetadatas) + 4 (one lazy query per
 * file) = 6 SELECTs and fails loudly - both on {@code @ExpectMaxSelect(3)}
 * and on the global {@code @DisableSameSelectTypesWithDifferentParamValues}
 * rule from {@code org.quickperf.QuickPerfConfiguration}.
 *
 * <p>{@code @JpaPerformanceTest} already implies {@code @QuickPerfTest} via
 * the meta-annotation (see patch 01), so no extra annotation is needed here.
 *
 * <p>Calibration: run once with {@code @AnalyzeSql} to see the real query
 * list. The number 3 is a deliberate contract - if a service legitimately
 * needs more, raise it in the test with a comment explaining why, never
 * silently.
 */
@JpaPerformanceTest
class FileMetadataReadBudgetIT {

    static JpaEntityManagerService jpa;
    static Long datasetVersionId;

    /** Seed size chosen so N+1 becomes 5+ SELECTs against a budget of 3. */
    static final int FILE_COUNT = 4;

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

        Dataset sampleDataset = fixture.dataset();
        jpa.inTransactionVoid(em -> {
            for (DataFile dataFile : fixture.dataFiles()) {
                em.persist(dataFile);
            }
            em.persist(sampleDataset);
        });

        datasetVersionId = sampleDataset.getVersions().get(0).getId();
    }

    @Test
    @DisplayName("Data service read budget: file metadata for 4 files costs at most 3 SELECTs, zero writes")
    @ExpectMaxSelect(3)
    @ExpectUpdate(0)
    @ExpectInsert(0)
    @ExpectDelete(0)
    void fileMetadataReadStaysWithinBudget() {
        jpa.inTransaction(em -> {
            DatasetVersion version = em.find(DatasetVersion.class, datasetVersionId);

            // What service callers do: iterate files and touch lazy file attributes.
            // DataFile.dataTables is a lazy @OneToMany (see DataFile.java) - without
            // a fetch plan this is one SELECT per file (N+1).
            for (FileMetadata fm : version.getFileMetadatas()) {
                fm.getDataFile().getDataTables().size();
            }
            return null;
        });
    }
}
