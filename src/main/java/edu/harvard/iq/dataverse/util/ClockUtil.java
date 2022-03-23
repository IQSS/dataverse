package edu.harvard.iq.dataverse.util;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;

@Singleton
public class ClockUtil {
    
    
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
    public @interface LocalTime {
    }
    
    @Produces
    @LocalTime
    public static final Clock LOCAL_CLOCK = Clock.systemDefaultZone();
    
}
