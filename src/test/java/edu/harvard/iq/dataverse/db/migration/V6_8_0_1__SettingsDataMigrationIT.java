package edu.harvard.iq.dataverse.db.migration;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.testing.Tags;
import org.dbunit.IDatabaseTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(Tags.DB_MIGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
class V6_8_0_1__SettingsDataMigrationIT {
    
    static final String MIGRATION_RESOURCE = "/db/migration/V6.8.0.1.sql";
    static final String TABLE_NAME = "setting";
    
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
        databaseTester = SharedPostgresContainer.getTester();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (databaseTester != null) {
            databaseTester.onTearDown();
        }
        // Clean up for the next test
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + TABLE_NAME + " RESTART IDENTITY CASCADE");
        }
    }
    
    @Test
    @DisplayName("Test migrating BuiltinUsers.KEY and Workflow settings")
    void testMigrationConvertSimpleSettings() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="1" name="BuiltinUsers.KEY" content="secret-key-123" />
                <setting id="2" name="WorkflowsAdmin#IP_WHITELIST_KEY" content="127.0.0.1" />
                <setting id="3" name="WorkflowServiceBean.WorkflowId:PrePublishDataset" content="1" />
                <setting id="4" name="WorkflowServiceBean.WorkflowId:PostPublishDataset" content="2" />
            </dataset>
            """;
        DBUnitHelper.loadData(databaseTester, inputXml);
        
        // WHEN
        DBUnitHelper.runMigrationScript(MIGRATION_RESOURCE);
        var actualData = DBUnitHelper.getActualData(databaseTester, TABLE_NAME, "name", "id");
        
        // THEN
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name="%s" content="secret-key-123" lang="[null]" />
                <setting name="%s" content="127.0.0.1" lang="[null]" />
                <setting name="%s" content="1" lang="[null]" />
                <setting name="%s" content="2" lang="[null]" />
            </dataset>
            """.formatted(Key.BuiltinUsersKey, Key.WorkflowsAdminIpWhitelist, Key.PrePublishDatasetWorkflowId, Key.PostPublishDatasetWorkflowId);
        var expectedData = DBUnitHelper.getExpectedData(expectedXml, TABLE_NAME, "name", "id");
        
        DBUnitHelper.assertTablesEqual(expectedData, actualData);
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
        DBUnitHelper.loadData(databaseTester, inputXml);
        
        // WHEN
        DBUnitHelper.runMigrationScript(MIGRATION_RESOURCE);
        var actualData = DBUnitHelper.getActualData(databaseTester, TABLE_NAME, "name", "id");
        
        // THEN
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name=":TabularIngestSizeLimit" content='{"csv": 5000, "tab": 1000, "default": 2000}' lang="[null]" />
            </dataset>
            """;
        var expectedData = DBUnitHelper.getExpectedData(expectedXml, TABLE_NAME, "name", "id");
        
        DBUnitHelper.assertTablesEqual(expectedData, actualData);
    }
    
    @Test
    @DisplayName("Test migrating TabularIngestSizeLimit handles NULL lang values")
    void testMigrationTabularIngestSizeLimitToJsonHandlesNullLangValues() throws Exception {
        // GIVEN
        var inputXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting id="1" name=":TabularIngestSizeLimit:csv" content="3000" />
            </dataset>
            """;
        DBUnitHelper.loadData(databaseTester, inputXml);
        
        // WHEN
        DBUnitHelper.runMigrationScript(MIGRATION_RESOURCE);
        var actualTable = DBUnitHelper.getActualData(databaseTester, TABLE_NAME, "name", "id");
        
        // THEN
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
        DBUnitHelper.loadData(databaseTester, inputXml);
        
        // WHEN
        DBUnitHelper.runMigrationScript(MIGRATION_RESOURCE);
        var actualData = DBUnitHelper.getActualData(databaseTester, TABLE_NAME, "name", "id");
        
        // THEN
        // Invalid values should be set to 0 (at least that's what the migration *should* do)
        // Notes: We don't specify 'id' in expected because they are auto-generated.
        //        We use single quotes for XML attributes containing JSON with double quotes.
        var expectedXml = """
            <?xml version='1.0' encoding='UTF-8'?>
            <dataset>
                <setting name=":TabularIngestSizeLimit" content='{"csv": 0}' lang="[null]" />
            </dataset>
            """;
        var expectedData = DBUnitHelper.getExpectedData(expectedXml, TABLE_NAME, "name", "id");
        
        DBUnitHelper.assertTablesEqual(expectedData, actualData);
    }
}