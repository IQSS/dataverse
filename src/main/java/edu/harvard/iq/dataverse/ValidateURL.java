
package edu.harvard.iq.dataverse;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class})
@Documented
public @interface ValidateURL {

    String message() default "Failed Validation for Validate URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
