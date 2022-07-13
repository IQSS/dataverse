package edu.harvard.iq.dataverse.util.testing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SystemProperty} is a JUnit Jupiter extension to set the value of an
 * arbitrary system property for a test execution.
 *
 * <p>The key and value of the property to be set must be specified via
 * {@link #key()} and {@link #value()}. After the annotated method has been
 * executed, the initial default value is restored.</p>
 *
 * <p>{@code SetJvmSetting} can be used on the method and on the class level.
 * It is repeatable and inherited from higher-level containers. If a class is
 * annotated, the configured property will be set before every test inside that
 * class. Any method level configurations will override the class level
 * configurations.</p>
 *
 * Parallel execution of tests using this extension is prohibited by using
 * resource locking provided by JUnit5 - system properties are a global state,
 * these tests NEED to be done serial.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@Repeatable(SystemProperty.SystemProperties.class)
@ExtendWith(SystemPropertyExtension.class)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
public @interface SystemProperty {
    /**
     * The key of the system property to be set.
     */
    String key();
    
    /**
     * The value of the system property to be set.
     */
    String value();
    
    /**
     * Containing annotation of repeatable {@code @SystemProperty}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Inherited
    @ExtendWith(SystemPropertyExtension.class)
    @interface SystemProperties {
        SystemProperty[] value();
    }
}