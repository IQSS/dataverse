package edu.harvard.iq.dataverse.persistence.config;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class})
@Documented
public @interface ValidateURL {

    String message() default "Failed Validation for Validate URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
