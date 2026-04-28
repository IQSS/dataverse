package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.db.performance.JpaTestBootstrap;
import edu.harvard.iq.dataverse.util.testing.Tags;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(Tags.USES_TESTCONTAINERS)
@Tag(Tags.PERFORMANCE_TEST)
@Testcontainers(disabledWithoutDocker = true)
class HugeDatasetExportPerformanceIT {
    
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    static JpaTestBootstrap jpa;
    
    static Dataset regularFilesDataset;
    
    @BeforeAll
    static void setUp() {
        postgres.start();
        jpa = new JpaTestBootstrap(postgres);
        jpa.start();
        
        DatasetRecipe regularFiles = DatasetRecipe.of(
            DatasetTypeRecipe.dataset(),
            VersionRecipe.of(
                FileRecipe.regular(1000)
            )
        );
        
        // Build the fixture
        var regularFixture = DatasetFixtureBuilder.builder().recipe(regularFiles).build();
        
        // Some entities need to be present in the database to appropriatly let the ORM create the mappings
        jpa.inTransactionVoid(em -> em.persist(regularFixture.datasetType()));
        
        // Persist the actual dataset
        regularFilesDataset = regularFixture.dataset();
        jpa.inTransactionVoid(em -> {
            // DataFile has no cascade path from Dataset, so each file must be persisted explicitly before
            // the dataset graph is flushed.
            for (DataFile dataFile : regularFixture.dataFiles()) {
                em.persist(dataFile);
            }
            em.persist(regularFilesDataset);
        });
    }
    
    @AfterAll
    static void tearDown() {
        if (jpa != null) {
            jpa.close();
        }
        postgres.stop();
    }
    
    @Test
    void shouldExportLargeDataset() {
        Long datasetVersionId = regularFilesDataset.getId();
        
        QueryCountHolder.clear();
        Instant start = Instant.now();
        
        String json = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB. Check fixtures!");
            
            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            var details = provider.getDatasetFileDetails();
            return details.toString();
        });
        
        assertNotNull(json);
        
        Instant end = Instant.now();
        
        QueryCount count = QueryCountHolder.getGrandTotal();
        System.out.println("Elapsed ms: " + start.until(end, ChronoUnit.MILLIS));
        System.out.println("Total queries: " + count.getTotal());
        System.out.println("Select queries: " + count.getSelect());
        System.out.println("Insert queries: " + count.getInsert());
        System.out.println("Update queries: " + count.getUpdate());
        System.out.println("Delete queries: " + count.getDelete());
    }
}
