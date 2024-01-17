
package edu.harvard.iq.dataverse.validation;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class})
@Documented
public @interface ValidateURL {
    String message() default "'${validatedValue}' {url.invalid}";
    String[] schemes() default {"http", "https", "ftp"};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
