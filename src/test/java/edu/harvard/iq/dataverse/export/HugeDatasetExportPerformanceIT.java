package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.db.performance.JpaTestBootstrap;
import edu.harvard.iq.dataverse.util.testing.Tags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(Tags.USES_TESTCONTAINERS)
@Tag(Tags.PERFORMANCE_TEST)
@Testcontainers(disabledWithoutDocker = true)
class HugeDatasetExportPerformanceIT {
    
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    static JpaTestBootstrap jpa;
    
    @BeforeAll
    static void setUp() {
        postgres.start();
        jpa = new JpaTestBootstrap(postgres);
        jpa.start();
        
        // TODO: run schema migration / load fixture here
        
        
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
        Long datasetVersionId = 123L;
        
        String json = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB. Check fixtures!");
            
            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            return provider.getDatasetFileDetails().toString();
        });
        
        assertNotNull(json);
    }
}
