package edu.harvard.iq.dataverse.engine.command;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Commands that affect more than a single dataverse, have to declare this
 * using this annotation. The named sets of permissions are tested against
 * the {@link Dataverse}s returned by the {@link Command#getAffectedDataverses()}
 * method.
 * 
 * @author michael
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiredPermissionsMap {
	RequiredPermissions[] value();
}
