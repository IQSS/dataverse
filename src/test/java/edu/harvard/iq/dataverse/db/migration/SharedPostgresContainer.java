package edu.harvard.iq.dataverse.db.migration;

import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton container that is started once and reused across all tests.
 */
public class SharedPostgresContainer {
    
    private static volatile PostgreSQLContainer instance;
    
    /**
     * Provides a singleton instance of {@link PostgreSQLContainer} configured with
     * a specific PostgreSQL version, database name, username, and password. The
     * container is created and started on first invocation and reused across
     * subsequent calls.
     *
     * @return the singleton {@link PostgreSQLContainer} instance for use in tests
     */
    public static PostgreSQLContainer getInstance() {
        if (instance == null) {
            synchronized (SharedPostgresContainer.class) {
                if (instance == null) {
                    String version = System.getProperty("postgresql.server.version");
                    System.out.println("Creating singleton Postgres container with version: " + version);
                    
                    instance = new PostgreSQLContainer("postgres:" + version)
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test");
                    
                    instance.start();
                    
                    // Ensure container is stopped when JVM shuts down
                    Runtime.getRuntime().addShutdownHook(new Thread(instance::stop));
                }
            }
        }
        return instance;
    }
    
    /**
     * Retrieves a database connection using the credentials and JDBC URL
     * provided by the singleton PostgreSQL container instance.
     *
     * @return a {@link Connection} object to interact with the PostgreSQL database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            getInstance().getJdbcUrl(),
            getInstance().getUsername(),
            getInstance().getPassword()
        );
    }
    
    /**
     * Creates and returns an initialized instance of IDatabaseTester for testing
     * database interactions. The tester is configured to work with a PostgreSQL
     * database by using a PostgresqlDataTypeFactory.
     *
     * @return an IDatabaseTester instance configured for the current PostgreSQL container
     * @throws ClassNotFoundException if there is an issue creating the database tester or connection
     */
    public static IDatabaseTester getTester() throws ClassNotFoundException {
        return new JdbcDatabaseTester(
            getInstance().getDriverClassName(),
            getInstance().getJdbcUrl(),
            getInstance().getUsername(),
            getInstance().getPassword()
        ) {
            @Override
            public IDatabaseConnection getConnection() throws Exception {
                IDatabaseConnection connection = super.getConnection();
                connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
                return connection;
            }
        };
    }
}
