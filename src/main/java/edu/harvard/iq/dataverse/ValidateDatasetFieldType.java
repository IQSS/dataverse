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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DatasetFieldValidator.class, DatasetFieldValueValidator.class})
@Documented
public @interface ValidateDatasetFieldType {

  String message() default "Failed Validation for DSFType";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}