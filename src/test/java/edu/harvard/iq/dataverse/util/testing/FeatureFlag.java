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
 * {@code @FeatureFlag} is a JUnit Jupiter extension to set the value of a
 * feature flag (internally a system property) for a test execution.
 *
 * <p>The name and state of the feature flag to be set must be specified via
 * {@link #flag()} and {@link #value()}. After the annotated method has been
 * executed, the initial default value is restored.</p>
 *
 * <p>{@code @FeatureFlag} can be used on the method and on the class level.
 * It is repeatable and inherited from higher-level containers. If a class is
 * annotated, the configured flag will be set before every test inside that
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
@Repeatable(FeatureFlag.FeatureFlags.class)
@ExtendWith(FeatureFlagExtension.class)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
public @interface FeatureFlag {
    
    /**
     * The name of the feature flag to be set.
     */
    edu.harvard.iq.dataverse.settings.FeatureFlags flag();
    
    /**
     * The state of the flag to be set.
     */
    boolean value() default true;
    
    /**
     * Containing annotation of repeatable {@code @FeatureFlag}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Inherited
    @ExtendWith(FeatureFlagExtension.class)
    @interface FeatureFlags {
        FeatureFlag[] value();
    }
}
