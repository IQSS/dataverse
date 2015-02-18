/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
/**
 *
 * @author skraffmi
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {EMailValidator.class})
@Documented
public @interface  ValidateEmail {
  String message() default "Failed Validation Email Address";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
    
}
