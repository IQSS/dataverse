package edu.harvard.iq.dataverse;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {FileLabelValidator.class})
@Documented
public @interface ValidateDataFileLabel {
    
    String message() default "Failed Validation for Validate Data File Label";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    
}
