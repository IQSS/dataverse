/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.user;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author sarahferry
 */

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {UserNameValidator.class})
@Documented
public @interface ValidateUserName {
    String message() default "Failed Validation Username";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

