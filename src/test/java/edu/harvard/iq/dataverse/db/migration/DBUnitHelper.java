package edu.harvard.iq.dataverse.db.migration;

import org.dbunit.Assertion;
import org.dbunit.IDatabaseTester;
import org.dbunit.assertion.DiffCollectingFailureHandler;
import org.dbunit.assertion.Difference;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

class DBUnitHelper {
    /**
     * Loads dataset from the provided XML string into the database associated with the given
     * {@link IDatabaseTester} instance. It performs the following steps:
     * - Parses the XML string into an {@link IDataSet}.
     * - Replaces occurrences of "[null]" with actual null values in the dataset.
     * - Inserts the data into the database, performing a CLEAN_INSERT operation.
     * - Updates database sequences to avoid primary key collisions.
     *
     * @param tester the {@link IDatabaseTester} instance used for database operations.
     * @param inputXml the XML string representing the dataset to be loaded into the database.
     * @throws Exception if an error occurs during dataset parsing, database setup, or sequence update.
     */
    public static void loadData(IDatabaseTester tester, String inputXml) throws Exception {
        try (InputStream xml = new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8))) {
            IDataSet inputDataSet = new FlatXmlDataSetBuilder().build(xml);
            
            // Handle NULL values by replacing [null] string with actual NULL in the input dataset
            ReplacementDataSet dataSet = new ReplacementDataSet(inputDataSet);
            dataSet.addReplacementObject("[null]", null);
            
            tester.setDataSet(dataSet);
            tester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
            tester.onSetup();
        }
        
        // Update the sequence to avoid primary key collisions with the data just inserted by DBUnit
        try (Connection conn = tester.getConnection().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT setval('setting_id_seq', (SELECT MAX(id) FROM setting))");
        }
    }
    
    /**
     * Retrieves and processes the actual data from a specified table in the database.
     * The method performs the following steps:
     * - Fetches the entire dataset from the database connection.
     * - Retrieves the specific table from the dataset.
     * - Filters out specified columns from the table.
     * - Sorts the table based on the provided column.
     *
     * @param tester the {@link IDatabaseTester} instance used to access the database.
     * @param table the name of the table to retrieve from the dataset.
     * @param orderBy the column name by which the table should be sorted.
     * @param ignoreCols optional columns to exclude from the resulting table.
     * @return a processed and sorted {@link ITable} object representing the actual data from the database table.
     * @throws Exception if an error occurs during database access, table filtering, or sorting.
     */
    public static ITable getActualData(IDatabaseTester tester, String table, String orderBy, String... ignoreCols) throws Exception {
        IDataSet actualDataSet = tester.getConnection().createDataSet();
        ITable actualTable = actualDataSet.getTable(table);
        ITable filteredActual = DefaultColumnFilter.excludedColumnsTable(actualTable, ignoreCols);
        SortedTable sortedActual = new SortedTable(filteredActual, new String[]{orderBy});
        sortedActual.setUseComparable(true);
        return sortedActual;
    }
    
    /**
     * Processes the given XML data to produce an expected dataset table for comparison purposes.
     * The method performs the following steps:
     * - Parses the XML data into an {@link IDataSet}.
     * - Replaces "[null]" strings with actual null values in the dataset.
     * - Filters out specified columns from the table.
     * - Sorts the resulting table based on the provided column(s).
     *
     * @param expectedXml the XML string representing the expected dataset.
     * @param table the name of the table to retrieve from the dataset.
     * @param orderBy the column name by which the table should be sorted.
     * @param ignoreCols optional columns to exclude from the resulting table.
     * @return a processed and sorted {@link ITable} object representing the expected state of the dataset.
     * @throws Exception if an error occurs during XML parsing, dataset processing, or table filtering.
     */
    public static ITable getExpectedData(String expectedXml, String table, String orderBy, String... ignoreCols) throws Exception {
        try (InputStream xml = new ByteArrayInputStream(expectedXml.getBytes(StandardCharsets.UTF_8))) {
            IDataSet rawDataSet = new FlatXmlDataSetBuilder().build(xml);
            
            // Handle NULL values by replacing [null] string with actual NULL in the expected dataset
            // This is necessary to deal with the lang column, which may contain NULL values.
            ReplacementDataSet expectedDataSet = new ReplacementDataSet(rawDataSet);
            expectedDataSet.addReplacementObject("[null]", null);
            ITable expectedTable = expectedDataSet.getTable(table);
            
            // Filter out the id column (potentially contains auto-generated values, not comparable)
            ITable filteredExpected = DefaultColumnFilter.excludedColumnsTable(expectedTable, ignoreCols);
            
            // Sort the resulting table by name (using comparable to avoid byte-wise sorting!)
            SortedTable sortedExpected = new SortedTable(filteredExpected, new String[]{orderBy});
            sortedExpected.setUseComparable(true);
            
            return sortedExpected;
        }
    }
    
    
    /**
     * Executes a database migration script located at the specified resource path.
     * This method loads the SQL script from the given path, establishes a connection
     * to the database via the {@link SharedPostgresContainer}, and executes the script.
     *
     * @param migrationResourcePath the path to the migration script resource to be executed
     * @throws Exception if an error occurs while loading the resource, establishing the database connection,
     *         or executing the SQL script
     */
    public static void runMigrationScript(String migrationResourcePath) throws Exception {
        String migrationSql = loadResource(migrationResourcePath);
        try (Connection conn = SharedPostgresContainer.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(migrationSql);
        }
    }
    
    /**
     * Loads the content of a resource file located at the given path into a string.
     * The resource is read with UTF-8 encoding, and if the resource is not found, an IOException
     * is thrown. Proper handling of the InputStream is ensured using try-with-resources.
     *
     * @param path the path to the resource file to be loaded
     *             (Note: a path starting with "/" is absolute (root of the classpath). Without it is relative to {@link DBUnitHelper})
     * @return the content of the resource as a string
     * @throws IOException if the resource cannot be found or an error occurs during reading
     */
    public static String loadResource(String path) throws IOException {
        try (InputStream is = DBUnitHelper.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Asserts that two database tables are equal. The method compares the
     * expected and actual tables row by row and column by column. If differences
     * are found, an {@link AssertionError} is thrown with detailed information
     * about the discrepancies, including the expected and actual values and
     * their corresponding XML representations.
     *
     * @param expected the expected {@link ITable} object, representing the
     *                 expected state of the database table.
     * @param actual   the actual {@link ITable} object, representing the actual
     *                 state of the database table to compare against the expected table.
     * @throws Exception if an error occurs during the comparison or while
     *                   generating the XML representation of the tables.
     */
    public static void assertTablesEqual(ITable expected, ITable actual) throws Exception {
        DiffCollectingFailureHandler failureHandler = new DiffCollectingFailureHandler();
        Assertion.assertEquals(expected, actual, failureHandler);
        
        if (!failureHandler.getDiffList().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DIFFERENCES FOUND ===\n");
            
            for (Object diff : failureHandler.getDiffList()) {
                Difference d = (Difference) diff;
                sb.append(String.format("Row %d, Column '%s': expected <%s> but was <%s>%n",
                    d.getRowIndex(),
                    d.getColumnName(),
                    d.getExpectedValue(),
                    d.getActualValue()));
            }
            
            sb.append("\n=== EXPECTED DATA ===\n");
            sb.append(tableToXmlString(expected));
            
            sb.append("\n=== ACTUAL DATA ===\n");
            sb.append(tableToXmlString(actual));
            
            throw new AssertionError(sb.toString());
        }
    }
    
    /**
     * Converts the data in the given {@link ITable} to an XML string representation.
     * This method serializes the specified table into a flat XML format using UTF-8 encoding.
     *
     * @param table the {@link ITable} to be converted to an XML string
     * @return a string containing the XML representation of the specified table
     * @throws Exception if an error occurs during the serialization process
     */
    public static String tableToXmlString(ITable table) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlatXmlDataSet.write(new DefaultDataSet(table), baos);
        return baos.toString(StandardCharsets.UTF_8);
    }
}