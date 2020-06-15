package edu.harvard.iq.dataverse.persistence.config.annotations;

import edu.harvard.iq.dataverse.persistence.config.EntityCustomizer;
import org.eclipse.persistence.annotations.Customizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used in conjunction with {@link Customizer}
 * from EclipseLink to modify query that is used by JPA to fetch data for the annotated field.
 * <p>The rationale for using this annotation is twofold:
 * <ol>
 * <li>{@link Customizer} is used on type level and the fact that
 * it is used for the given field can be easily overlooked.
 * <li>Using annotation enables the proper customization code to get rid of hardcoded field names and
 * evade errors that would stem from accidental field name change.
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CustomizeSelectionQuery {
    EntityCustomizer.Customizations value();
}
