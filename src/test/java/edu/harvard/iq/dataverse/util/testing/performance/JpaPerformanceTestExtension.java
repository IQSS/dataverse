package edu.harvard.iq.dataverse.util.testing.performance;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static java.lang.reflect.Modifier.isStatic;

/**
 * JUnit 5 Extension that manages a shared PostgreSQL container for performance tests.
 * It ensures a unique database is created for each test class to guarantee isolation.
 */
public class JpaPerformanceTestExtension implements BeforeAllCallback, AfterAllCallback {

    // Global shared container
    private static PostgreSQLContainer sharedContainer;
    
    // This lock makes sure all tests using this extension are executed sequentially.
    // For performance tests, executing test classes in parallel for the same, shared DB instance makes no sense.
    // There is no JUnit way to express such a "global lock", thus we need to do this manually.
    // Note: avoiding parallelism of test methods are done by the @JpaPerformanceTest annotation.
    private static final Object CONTAINER_LOCK = new Object();
    
    // Store the service instance to close it in AfterAll
    private static final String SERVICE_FIELD_KEY = "jpa.service.instance";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // 1. Ensure the global container is running
        ensureSharedContainerRunning();

        // 2. Create a unique database for this test class
        String uniqueDbName = "perf_test_" + UUID.randomUUID().toString().substring(0, 8);
        createDatabase(uniqueDbName);
        
        // 3. Retrieve the JPA Service and inject into the test class field
        JpaEntityManagerService service = getService(uniqueDbName);
        injectService(context, service);
        
        // 4. Store reference for cleanup
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(SERVICE_FIELD_KEY, service);
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        // Close the EntityManagerFactory and connections
        JpaEntityManagerService service = (JpaEntityManagerService) context.getStore(ExtensionContext.Namespace.GLOBAL).get(SERVICE_FIELD_KEY);
        if (service != null) {
            try {
                service.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Note: We do NOT stop the sharedContainer here. 
        // It stays running for the next test class.
    }

    // --- Helper Methods ---

    private void ensureSharedContainerRunning() {
        synchronized (CONTAINER_LOCK) {
            if (sharedContainer == null || !sharedContainer.isRunning()) {
                String pgVersion = System.getProperty("postgresql.server.version", "16");
                sharedContainer = new PostgreSQLContainer("postgres:" + pgVersion);
                sharedContainer.start();
            }
        }
    }

    private void createDatabase(String dbName) {
        try (Connection conn = DriverManager.getConnection(
                sharedContainer.getJdbcUrl(), 
                sharedContainer.getUsername(), 
                sharedContainer.getPassword())) {
            
            // Postgres requires auto-commit to be true for CREATE DATABASE
            conn.setAutoCommit(true);
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE " + dbName);
        } catch (SQLException e) {
            // Ignore if DB already exists (unlikely with UUID, but safe)
            if (!e.getMessage().contains("already exists")) {
                throw new RuntimeException("Failed to create test database: " + dbName, e);
            }
        }
    }

    private void injectService(ExtensionContext context, JpaEntityManagerService service) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        boolean hasBeenInjected = false;
        
        // Look for a static field of type JpaService
        for (Field field : testClass.getDeclaredFields()) {
            if (field.getType() == JpaEntityManagerService.class) {
                if (!isStatic(field.getModifiers())) {
                    throw new RuntimeException("Cannot inject into field '" + field.getName() + "' of class '" + testClass.getName() + "': not a static field");
                }
                if (hasBeenInjected) {
                    throw new RuntimeException("Cannot inject into field '" + field.getName() + "' of class '" + testClass.getName() + "': only one target field allowed");
                }
                field.setAccessible(true);
                field.set(null, service);
                hasBeenInjected = true;
            }
        }
        
        if (!hasBeenInjected) {
            throw new RuntimeException("Could not inject into a static field of class '" + testClass.getName() + "': no field found");
        }
    }
    
    private static JpaEntityManagerService getService(String uniqueDbName) {
        // Tune the URL as we need to apply our unique DB name (the container has a default one we override)
        String tunedJdbcUrl = sharedContainer.getJdbcUrl()
            .replaceFirst("/" + sharedContainer.getDatabaseName(), "/" + uniqueDbName);
        
        // Configure a pooled (!) DataSource for this unique database
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(tunedJdbcUrl);
        dataSource.setUsername(sharedContainer.getUsername());
        dataSource.setPassword(sharedContainer.getPassword());
        
        return new JpaEntityManagerService(dataSource);
    }
}
