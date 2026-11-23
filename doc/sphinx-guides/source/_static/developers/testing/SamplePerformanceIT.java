package edu.harvard.iq.dataverse.somepackage;

import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Single annotation for automatic setup of
//     1) basic tags for JUnit groups,
//     2) shared PostgreSQL server via Testcontainers, and
//     3) creation and injection of JPA entity manager service.
@JpaPerformanceTest
class SamplePerformanceIT {
    
    static JpaEntityManagerService jpa;
    
    @BeforeAll
    static void setUp() {
        // A manual start is necessary to allow you to selectively enable service features as necessary
        jpa.start();
        
        // inTransactionVoid: Use this when you only need to execute database operations
        // (e.g., persisting test fixtures) without returning a value.
        jpa.inTransactionVoid(em -> {
            // EntityManager em is provided here.
            // em.persist(myEntity);
        });
    }
    
    @Test
    void shouldMeasureOperationPerformance() {
        // Clear any previous query statistics
        QueryCountHolder.clear();
        Instant start = Instant.now();
        
        // inTransaction: Use this when your operation returns a result that needs
        // to be asserted or measured.
        Object result = jpa.inTransaction(em -> {
            // Execute your performance-critical operation using the EntityManager.
            // return result;
            return null; // Placeholder
        });
        
        Instant end = Instant.now();
        assertNotNull(result);
        
        // Retrieve and log ORM statistics
        QueryCount count = QueryCountHolder.getGrandTotal();
        System.out.println("Elapsed ms: " + start.until(end, ChronoUnit.MILLIS));
        System.out.println("Total queries: " + count.getTotal());
        System.out.println("Select queries: " + count.getSelect());
        System.out.println("Insert queries: " + count.getInsert());
        System.out.println("Update queries: " + count.getUpdate());
        System.out.println("Delete queries: " + count.getDelete());
    }
}