package edu.harvard.iq.dataverse.db.performance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class JpaTestBootstrap implements AutoCloseable {
    
    public static final String PERSISTENCE_UNIT = "VDCNet-ejbPU-test";
    
    private final PostgreSQLContainer postgres;
    
    private DataSource dataSource;
    private EntityManagerFactory emf;
    
    public JpaTestBootstrap(PostgreSQLContainer postgres) {
        this.postgres = postgres;
    }
    
    public void start() {
        if (emf != null) {
            throw new IllegalStateException("JpaTestBootstrap has already been started.");
        }
        
        DataSource baseDataSource = createDataSource();
        dataSource = ProxyDataSourceBuilder.create()
            .dataSource(baseDataSource)
            .countQuery()
            .buildProxy();
        
        validateDataSource(dataSource);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.nonJtaDataSource", dataSource);
        properties.put("jakarta.persistence.schema-generation.database.action", "create");
        
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
        
        validateEntityManagerFactory();
    }
    
    public DataSource getDataSource() {
        ensureStarted();
        return dataSource;
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
    
    private DataSource createDataSource() {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setURL(postgres.getJdbcUrl());
        pgDataSource.setUser(postgres.getUsername());
        pgDataSource.setPassword(postgres.getPassword());
        return pgDataSource;
    }
    
    private void validateDataSource(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new IllegalStateException("DataSource connection is not valid.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate DataSource.", e);
        }
    }
    
    private void validateEntityManagerFactory() {
        EntityManager entityManager = emf.createEntityManager();
        entityManager.close();
    }
    
    private void ensureStarted() {
        if (emf == null) {
            throw new IllegalStateException("JpaTestBootstrap has not been started yet.");
        }
    }
    
    @Override
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = null;
        dataSource = null;
    }
}