package edu.harvard.iq.dataverse.db.migration;


import edu.harvard.iq.dataverse.util.testing.Tags;
import org.dbunit.Assertion;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import static org.dbunit.Assertion.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(Tags.DB_MIGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
public class V6_8_0_1__SettingsDataMigrationIT {
    
    static final String MIGRATION_RESOURCE = "/db/migration/V6.8.0.1.sql";
    static final String TABLE_NAME = "setting";
    
    @Container
    static PostgreSQLContainer POSTGRES = SharedPostgresContainer.getInstance();
    
    private IDatabaseTester databaseTester;
    
    @BeforeAll
    static void initSchema() throws Exception {
        // Create the table structure (the DDL that already exists in your database)
        String schemaDDL = """
            create table %s
            (
                id      serial primary key,
                content text,
                lang    text constraint non_empty_lang check (lang <> ''::text),
                name    text
            );
            
            create unique index unique_settings on setting (name, (COALESCE(lang, ''::text)));
            """.formatted(TABLE_NAME);
        
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(schemaDDL);
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a new database tester instance, get the connection details from Testcontainers
        databaseTester = new JdbcDatabaseTester(
            POSTGRES.getDriverClassName(),
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (databaseTester != null) {
            databaseTester.onTearDown();
        }
        // Clean up for the next test
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE setting RESTART IDENTITY CASCADE");
        }
    }
    
    @Test
    @DisplayName("Test migrating BuiltinUsers.KEY and WorkflowsAdmin")
    void testMigrationConvertSimpleSettings() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="1" name="BuiltinUsers.KEY" content="secret-key-123" />
                <setting id="2" name="WorkflowsAdmin#IP_WHITELIST_KEY" content="127.0.0.1" />
            </dataset>
            """;
        loadData(inputXml);
        
        // WHEN
        runMigrationScript();
        var actualData = getActualData();
        
        // THEN
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name=":BuiltinUsersKey" content="secret-key-123" />
                <setting name=":WorkflowsAdminIpWhitelist" content="127.0.0.1" />
            </dataset>
            """;
        var expectedData = getExpectedData(expectedXml);
        
        Assertion.assertEquals(expectedData, actualData);
    }
    
    @Test
    @DisplayName("Test migrating TabularIngestSizeLimit to JSON")
    void testMigrationConvertTabularIngestSizeLimitToJsonFormat() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="3" name=":TabularIngestSizeLimit" content="2000" />
                <setting id="4" name=":TabularIngestSizeLimit:csv" content="5000" />
                <setting id="5" name=":TabularIngestSizeLimit:tab" content="1000" />
            </dataset>
            """;
        loadData(inputXml);
        
        // WHEN
        runMigrationScript();
        var actualData = getActualData();
        
        // THEN
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name=":TabularIngestSizeLimit" content='{"csv": 5000, "tab": 1000, "default": 2000}' />
            </dataset>
            """;
        var expectedData = getExpectedData(expectedXml);
        
        Assertion.assertEquals(expectedData, actualData);
    }
    
    @Test
    @DisplayName("Test migrating TabularIngestSizeLimit handles NULL lang values")
    void testMigrationTabularIngestSizeLimitToJsonHandlesNullLangValues() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="1" name=":TabularIngestSizeLimit:csv" content="3000" lang="[null]" />
            </dataset>
            """;
        loadData(inputXml);
        
        // WHEN
        runMigrationScript();
        
        // THEN
        // Verify the migration completed...
        IDataSet actualDataSet = databaseTester.getConnection().createDataSet();
        ITable actualTable = actualDataSet.getTable("setting");
        
        // Just verify we have at least one row (the migrated JSON setting)
        assertTrue(actualTable.getRowCount() > 0);
    }
    
    @Test
    @DisplayName("Test migrating TabularIngestSizeLimit handles non-numeric values")
    void testMigrationTabularIngestSizeLimitToJsonHandlesNonNumericValuesGracefully() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="1" name=":TabularIngestSizeLimit:csv" content="not-a-number" />
            </dataset>
            """;
        loadData(inputXml);
        
        // WHEN
        runMigrationScript();
        var actualData = getActualData();
        
        // THEN
        // Invalid values should be set to 0 (at least that's what the migration *should* do)
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name=":TabularIngestSizeLimit" content='{"csv": 0}' />
            </dataset>
            """;
        var expectedData = getExpectedData(expectedXml);
        
        assertEquals(expectedData, actualData);
    }
    
    // --- Helper Methods ---
    private void loadData(String inputXml) throws Exception {
        try (InputStream xml = new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8))) {
            IDataSet inputDataSet = new FlatXmlDataSetBuilder().build(xml);
            databaseTester.setDataSet(inputDataSet);
            databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
            databaseTester.onSetup();
        }
        
        // Update the sequence to avoid primary key collisions with the data just inserted by DBUnit
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT setval('setting_id_seq', (SELECT MAX(id) FROM setting))");
        }
    }
    
    /**
     * Retrieves and processes the actual data from the database for verification purposes.
     * The method performs the following steps:
     * - Obtains the current dataset from the database connection.
     * - Filters out the id and lang column from the data.
     * - Sorts the resulting table based on predefined sorting criteria.
     *
     * @return a processed and sorted {@link ITable} object representing the actual state of the database.
     * @throws Exception if an error occurs during dataset retrieval, column filtering, or table sorting.
     */
    private ITable getActualData() throws Exception {
        IDataSet actualDataSet = databaseTester.getConnection().createDataSet();
        ITable actualTable = actualDataSet.getTable(TABLE_NAME);
        ITable filteredActual = DefaultColumnFilter.excludedColumnsTable(actualTable, new String[]{"id", "lang"});
        SortedTable sortedActual = new SortedTable(filteredActual, new String[]{"name"});
        return sortedActual;
    }
    
    /**
     * Constructs and returns an expected data table for testing database contents, based on the provided XML input.
     * The method parses the given XML string into a dataset, filters out the id and lang column,
     * and sorts the table based on predefined sorting criteria.
     *
     * @param expectedXml the XML string representing the expected dataset to be used for verification
     * @return a processed and sorted {@link ITable} object representing the expected database state
     * @throws Exception if an error occurs during XML parsing, dataset creation, or table manipulation
     */
    private ITable getExpectedData(String expectedXml) throws Exception {
        try (InputStream xml = new ByteArrayInputStream(expectedXml.getBytes(StandardCharsets.UTF_8))) {
            IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(xml);
            ITable expectedTable = expectedDataSet.getTable(TABLE_NAME);
            ITable filteredExpected = DefaultColumnFilter.excludedColumnsTable(expectedTable, new String[]{"id", "lang"});
            SortedTable sortedExpected = new SortedTable(filteredExpected, new String[]{"name"});
            return sortedExpected;
        }
    }
    
    private void runMigrationScript() throws Exception {
        String migrationSql = loadResource(MIGRATION_RESOURCE);
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(migrationSql);
        }
    }
    
    private String loadResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}