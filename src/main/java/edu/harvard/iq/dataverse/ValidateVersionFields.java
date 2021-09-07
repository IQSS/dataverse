
package edu.harvard.iq.dataverse;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 *
 * @author skraffmi
 */
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DatasetVersionFieldsValidator.class})
@Documented
public @interface ValidateVersionFields {

    String message() default "Failed Validation Dataset Version";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
