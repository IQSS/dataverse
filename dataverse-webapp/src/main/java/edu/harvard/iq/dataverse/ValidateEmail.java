/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author skraffmi
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {EMailValidator.class})
@Documented
public @interface ValidateEmail {
    String message() default "Failed Validation Email Address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
