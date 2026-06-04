package edu.harvard.iq.dataverse.util.testing.performance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Service class managing the lifecycle and operations of an {@link EntityManagerFactory}
 * for JPA-based persistence. This class is responsible for configuring the persistence
 * unit, initializing the factory, and providing utility methods to interact with JPA
 * entities within transactions.
 *
 * Implementation contracts:
 * - The service must be explicitly started with the {@code start()} method before usage.
 * - Resources are properly released when the service is closed via the {@code close()} method.
 * - Transactions are managed and isolated when executing database operations.
 *
 * Use cases:
 * - Configure and initialize an {@link EntityManagerFactory} with a non-JTA datasource.
 * - Manage entity operations within transactions, supporting both functional and void work units.
 * - Validate the underlying datasource and factory to ensure system integrity.
 */
public class JpaEntityManagerService implements AutoCloseable {
    
    public static final String PERSISTENCE_UNIT = "VDCNet-ejbPU-test";
    
    private final DataSource baseDataSource;
    private DataSource proxiedDataSource;
    private EntityManagerFactory emf;
    
    public JpaEntityManagerService(DataSource dataSource) {
        this.baseDataSource = dataSource;
    }
    
    public void start() {
        if (emf != null) {
            throw new IllegalStateException("JpaEntityManagerService has already been started.");
        }
        
        proxiedDataSource = ProxyDataSourceBuilder.create()
            .dataSource(baseDataSource)
            .countQuery()
            .buildProxy();
        
        validateDataSource(proxiedDataSource);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.nonJtaDataSource", proxiedDataSource);
        properties.put("jakarta.persistence.schema-generation.database.action", "create");
        
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
        
        validateEntityManagerFactory();
    }
    
    public DataSource getDataSource() {
        ensureStarted();
        return proxiedDataSource;
    }
    
    public EntityManager createEntityManager() {
        ensureStarted();
        return emf.createEntityManager();
    }
    
    public EntityManagerFactory getEntityManagerFactory() {
        ensureStarted();
        return emf;
    }
    
    public <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void inTransactionVoid(Consumer<EntityManager> work) {
        inTransaction(em -> {
            work.accept(em);
            return null;
        });
    }
    
    private void validateDataSource(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new IllegalStateException("DataSource connection is not valid");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate DataSource", e);
        }
    }
    
    private void validateEntityManagerFactory() {
        EntityManager entityManager = emf.createEntityManager();
        entityManager.close();
    }
    
    private void ensureStarted() {
        if (emf == null) {
            throw new IllegalStateException("JpaEntityManagerService has not been started yet - did you run .start()?");
        }
    }
    
    @Override
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = null;
        proxiedDataSource = null;
    }
}