/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.validation;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
/**
 *
 * @author skraffmi
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {EMailValidator.class})
@Documented
public @interface ValidateEmail {
  String message() default "'${validatedValue}' {email.invalid}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
    
}
