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
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DatasetVersionValidator.class})
@Documented
public @interface  ValidateDatasetVersion {
  String message() default "Failed Validation Dataset Version";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
    
}
