package edu.harvard.iq.dataverse.api.dto.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {MailDomainListValidator.class})
@Documented
public @interface ValidMainDomainList {

    String message() default "Elements of domain list cannot be null, empty, or contain characters other than " +
            "lower/uppercase letters, digits, dot (.) and hyphen (-)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
