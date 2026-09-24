package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionFilesServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileSearchCriteria;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Working with the files of a dataset version must not run database queries per file or per variable:
 * for datasets with tens of thousands of files that takes many seconds (see #12739). Each test runs an
 * operation on a smaller and a larger dataset; the number of queries must not grow with the dataset.
 */
@JpaPerformanceTest
class DatasetVersionFileMetadatasPerformanceIT {

    /** Extra select queries allowed for the larger dataset; batches hold 500 items, more than these datasets have */
    static final long MAX_EXTRA_QUERIES = 5;

    static JpaEntityManagerService jpa;

    static DatasetType datasetType;
    static Long smallRegularVersion;
    static Long largeRegularVersion;
    static Long smallTabularVersion;
    static Long largeTabularVersion;
    /** Dataset id of each version */
    static final Map<Long, Long> datasetIds = new HashMap<>();

    @BeforeAll
    static void setUp() {
        jpa.start();
        // the schema.org JSON-LD includes the installation name
        BrandingUtilTest.setupMocks();
        smallRegularVersion = persistVersion(FileRecipe.regular(50));
        largeRegularVersion = persistVersion(FileRecipe.regular(100));
        smallTabularVersion = persistVersion(FileRecipe.tabular(5, VariableSetRecipe.uniform(10)));
        largeTabularVersion = persistVersion(FileRecipe.tabular(10, VariableSetRecipe.uniform(20)));
    }

    static Long persistVersion(FileRecipe files) {
        DatasetTypeRecipe typeRecipe = datasetType == null ? DatasetTypeRecipe.dataset() : DatasetTypeRecipe.of(datasetType);
        var fixture = DatasetFixtureBuilder.builder().recipe(DatasetRecipe.of(typeRecipe, VersionRecipe.of(files))).build();
        if (datasetType == null) {
            datasetType = fixture.datasetType();
            jpa.inTransactionVoid(em -> em.persist(datasetType));
        }
        Dataset dataset = fixture.dataset();
        jpa.inTransactionVoid(em -> {
            // DataFile has no cascade path from Dataset
            for (DataFile dataFile : fixture.dataFiles()) {
                dataFile.setOwner(dataset);
                em.persist(dataFile);
            }
            em.persist(dataset);
        });
        Long versionId = dataset.getVersions().get(0).getId();
        datasetIds.put(versionId, dataset.getId());
        return versionId;
    }

    /** Select queries of the operation on the object as a page gets it: its files are not loaded yet */
    static <T> long selectQueries(Long versionId, BiFunction<EntityManager, Long, T> load, Consumer<T> operation) {
        QueryCountHolder.clear();
        jpa.inTransactionVoid(em -> operation.accept(load.apply(em, versionId)));
        return QueryCountHolder.getGrandTotal().getSelect();
    }

    static void assertNoQueriesPerItem(String name, Long smallVersion, Long largeVersion, Consumer<DatasetVersion> operation) {
        assertNoQueriesPerItem(name, smallVersion, largeVersion, (em, id) -> em.find(DatasetVersion.class, id), operation);
    }

    static <T> void assertNoQueriesPerItem(String name, Long smallVersion, Long largeVersion,
            BiFunction<EntityManager, Long, T> load, Consumer<T> operation) {
        long small = selectQueries(smallVersion, load, operation);
        long large = selectQueries(largeVersion, load, operation);
        System.out.println(name + ": " + small + " select queries for the smaller dataset, " + large + " for the larger one");
        assertTrue(large - small <= MAX_EXTRA_QUERIES,
            name + ": select queries grow with the number of files or variables (" + small + " for the smaller dataset, " + large + " for the larger one)");
    }

    @Test
    void checkingForRestrictedFiles() {
        assertNoQueriesPerItem("restricted files", smallRegularVersion, largeRegularVersion, DatasetVersion::isHasRestrictedFile);
    }

    @Test
    void exportingFileDetails() {
        assertNoQueriesPerItem("export file details", smallRegularVersion, largeRegularVersion,
            version -> new InternalExportDataProvider(version).getDatasetFileDetails());
    }

    @Test
    void exportingTabularFileDetails() {
        assertNoQueriesPerItem("export tabular file details", smallTabularVersion, largeTabularVersion,
            version -> new InternalExportDataProvider(version).getDatasetFileDetails());
    }

    @Test
    void listingFilesAsJson() {
        assertNoQueriesPerItem("file listing json", smallTabularVersion, largeTabularVersion,
            version -> JsonPrinter.jsonFileMetadatas(version.getFileMetadatas()).build());
    }

    @Test
    void buildingSchemaDotOrgJsonLd() {
        assertNoQueriesPerItem("schema.org json-ld", smallRegularVersion, largeRegularVersion, DatasetVersion::getJsonLd);
    }

    /**
     * The dataset page loads its version with DatasetVersionServiceBean.findDeep, which runs this query, and then
     * reads these relations of every file. Joining the files in the query fails with batch fetching (EclipseLink 6169).
     */
    @Test
    void loadingVersionForDatasetPage() {
        BiFunction<EntityManager, Long, DatasetVersion> findDeep = (em, id) -> em
            .createNamedQuery("DatasetVersion.findById", DatasetVersion.class).setParameter("id", id).getSingleResult();
        Consumer<DatasetVersion> readFiles = version -> version.getFileMetadatas().forEach(fmd -> {
            DataFile dataFile = fmd.getDataFile();
            fmd.getCategories().size();
            dataFile.getIngestRequest();
            dataFile.getThumbnailForDataset();
            dataFile.getEmbargo();
            dataFile.getRetention();
            dataFile.getReleaseUser();
            dataFile.getCreator();
            dataFile.getDataTables().size();
            dataFile.getTags().size();
        });
        assertNoQueriesPerItem("dataset page version", smallRegularVersion, largeRegularVersion, findDeep, readFiles);
        assertNoQueriesPerItem("dataset page tabular version", smallTabularVersion, largeTabularVersion, findDeep, readFiles);
    }

    /**
     * The dataset API loads a dataset with DatasetServiceBean.findDeep, which runs this query; some callers then read
     * these relations of every file. Joining the files in the query fails with batch fetching (EclipseLink 6169).
     */
    @Test
    void loadingDatasetDeep() {
        BiFunction<EntityManager, Long, Dataset> findDeep = (em, versionId) -> em
            .createNamedQuery("Dataset.findById", Dataset.class).setParameter("id", datasetIds.get(versionId)).getSingleResult();
        Consumer<Dataset> readFiles = dataset -> {
            assertTrue(!dataset.getFiles().isEmpty(), "the dataset has no files");
            dataset.getFiles().forEach(dataFile -> {
                dataFile.getStorageQuota();
                dataFile.getIngestRequest();
                dataFile.getThumbnailForDataset();
                dataFile.getEmbargo();
                dataFile.getRetention();
                dataFile.getReleaseUser();
                dataFile.getCreator();
                dataFile.getDataTables().size();
                dataFile.getTags().size();
            });
        };
        assertNoQueriesPerItem("dataset deep", smallRegularVersion, largeRegularVersion, findDeep, readFiles);
        assertNoQueriesPerItem("dataset deep tabular", smallTabularVersion, largeTabularVersion, findDeep, readFiles);
    }

    /**
     * The file listing API (GET /api/datasets/{id}/versions/{v}/files) loads the file metadatas with this query
     * and prints them as JSON: the batch fetching applies to any query that returns file metadatas.
     */
    @Test
    void listingFilesForApi() {
        BiFunction<EntityManager, Long, List<FileMetadata>> listFiles = (em, id) -> {
            DatasetVersionFilesServiceBean files = new DatasetVersionFilesServiceBean();
            files.injectEntityManager(em);
            return files.getFileMetadatas(em.find(DatasetVersion.class, id), null, null,
                new FileSearchCriteria(null, null, null, null, null), DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ);
        };
        Consumer<List<FileMetadata>> printJson = fileMetadatas -> JsonPrinter.jsonFileMetadatas(fileMetadatas).build();
        assertNoQueriesPerItem("file listing api", smallRegularVersion, largeRegularVersion, listFiles, printJson);
        assertNoQueriesPerItem("file listing api tabular", smallTabularVersion, largeTabularVersion, listFiles, printJson);
    }
}
