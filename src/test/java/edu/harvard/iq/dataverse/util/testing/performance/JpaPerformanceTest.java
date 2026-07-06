package edu.harvard.iq.dataverse.util.testing.performance;

import edu.harvard.iq.dataverse.util.testing.Tags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for JPA performance tests using Testcontainers.
 * <p>
 * Applies automatic tags, enforces Testcontainers availability (skips if Docker is missing),
 * and registers a custom extension to manage a shared PostgreSQL container and database isolation.
 * <p>
 * <b>Contract:</b> Test classes using this annotation MUST declare a {@code static JpaEntityManagerService} field.
 * <b>Note:</b> Due to the underlying extension's shared container management, test classes annotated with this
 * will execute sequentially to prevent container state races.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag(Tags.PERFORMANCE_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(JpaPerformanceTestExtension.class)
// Make sure the test methods are never run in parallel - this would be bad for a performance test...
@Execution(ExecutionMode.SAME_THREAD)
public @interface JpaPerformanceTest {
}
