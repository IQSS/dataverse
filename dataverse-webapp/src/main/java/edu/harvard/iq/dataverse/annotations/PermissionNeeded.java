package edu.harvard.iq.dataverse.annotations;

import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to provide data for @Restricted annotation. It must be placed both inside @Restricted
 * annotation and on affected PARAMETERS.
 * <br>
 * The usage is following: we place the annotation inside @Restricted annotation (possibly multiple times) to configure
 * needed permissions. If there are more than one annotation on this level, attribute <b>on</b> should be used to
 * differentiate between target parameters – and in this case corresponding parameters should have the same <b>value</b>
 * on PARAMETER level annotation. If there is no <b>on</b> attribute filled it will be used for every parameter without
 * filled <b>value</b>. For example:<br>
 * <pre>
 * &#64;Restricted({
 *  &#64;PermissionNeeded(on = "source", needs = {Permission.EditDataset}),
 *  &#64;PermissionNeeded(needs = {Permission.AddDataset})
 * })
 * public void copyFromAnother(@PermissionNeeded Dataverse target, @PermissionNeeded("source") Dataset source) {
 *     ...
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionNeeded {
    String value() default "";
    String on() default "";
    Permission[] needs() default { };
    Permission[] needsOnOwner() default { };
    boolean allRequired() default false;
}
