package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.FileItem;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Include;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Order;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.TreeQuery;
import edu.harvard.iq.dataverse.util.testing.Tags;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixture;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTestExtension;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(JpaPerformanceTestExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class DatasetVersionTreeServiceIT {

    static JpaEntityManagerService jpa;
    private static DatasetFixture fixture;

    @BeforeAll
    static void setUp() {
        jpa.start();
        fixture = DatasetFixtureBuilder.builder().recipe(DatasetRecipe.of(
                DatasetTypeRecipe.dataset(), VersionRecipe.of(
                        FileRecipe.tabular(1, VariableSetRecipe.uniform(1)),
                        FileRecipe.regular(1)))).build();
        fixture.dataFiles().get(1).setContentType("text/plain");
        fixture.fileMetadatas().get(1).setLabel("file-1.txt");
        jpa.inTransactionVoid(em -> em.persist(fixture.datasetType()));
        jpa.inTransactionVoid(em -> {
            for (DataFile file : fixture.dataFiles()) {
                em.persist(file);
            }
            em.persist(fixture.dataset());
        });
    }

    @ParameterizedTest
    @CsvSource({
            "application/x-stata, application/x-stata",
            "text/csv, text/csv",
            "application/x-dvn-csvspss-zip, application/zip",
            "application/x-dvn-tabddi-zip, application/zip",
            "'', application/x-unknown",
            ", application/x-unknown"
    })
    void contentTypeFollowsDownloadFormat(String originalFormat, String expectedType)
            throws ReflectiveOperationException {
        try (EntityManager em = jpa.createEntityManager()) {
            DatasetVersionTreeService service = new DatasetVersionTreeService();
            var field = DatasetVersionTreeService.class.getDeclaredField("em");
            field.setAccessible(true);
            field.set(service, em);
            em.getTransaction().begin();
            try {
                em.find(DataTable.class, fixture.dataTables().getFirst().getId())
                        .setOriginalFileFormat(originalFormat);
                em.flush();

                for (boolean originals : new boolean[]{false, true}) {
                    var page = service.listChildren(fixture.currentVersion(),
                            new TreeQuery(null, null, null, Include.FILES, Order.NAME_AZ, originals));
                    assertEquals(2, page.items.size());
                    assertNull(page.nextCursor);
                    FileItem tabular = assertInstanceOf(FileItem.class, page.items.get(0));
                    assertEquals(originals ? expectedType : "text/tab-separated-values", tabular.contentType);
                    assertEquals("file-0.tab", tabular.name);
                    assertEquals("file-0.tab", tabular.path);
                    assertEquals(originals ? 2048L : 1024L, tabular.size);
                    assertEquals("public", tabular.access);
                    assertEquals(originals ? "fixture-checksum-0" : null, tabular.checksumValue);

                    FileItem regular = assertInstanceOf(FileItem.class, page.items.get(1));
                    assertEquals("text/plain", regular.contentType);
                    assertEquals("file-1.txt", regular.name);
                    assertEquals(1025L, regular.size);
                    assertEquals("fixture-checksum-1", regular.checksumValue);
                }
            } finally {
                em.getTransaction().rollback();
            }
        }
    }
}
