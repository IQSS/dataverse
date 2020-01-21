package edu.harvard.iq.dataverse.interceptors;

import edu.harvard.iq.dataverse.annotations.PermissionNeeded;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This interceptor is used to check whether the user executing annotated service method has proper permissions
 * to operate on certain data objects (dataverses, datasets and so on) that would be affected by the execution.
 * It is used with @PermissionNeeded annotation,
 * see {@link edu.harvard.iq.dataverse.annotations.PermissionNeeded}.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Restricted {
    @Nonbinding PermissionNeeded[] value() default { };
}
