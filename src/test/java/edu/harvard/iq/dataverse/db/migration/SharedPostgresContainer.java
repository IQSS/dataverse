package edu.harvard.iq.dataverse.db.migration;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Singleton container that is started once and reused across all tests.
 */
public class SharedPostgresContainer {
    
    private static volatile PostgreSQLContainer instance;
    
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
    
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(
            getInstance().getJdbcUrl(),
            getInstance().getUsername(),
            getInstance().getPassword()
        );
    }
}
