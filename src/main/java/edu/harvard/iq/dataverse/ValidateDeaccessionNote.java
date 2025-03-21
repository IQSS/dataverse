/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 *
 * @author skraffmi
 */


@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DatasetDeaccessionNoteValidator.class})
@Documented
public @interface ValidateDeaccessionNote {

  String message() default "Failed Validation for DatasetsDeaccessionNote";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
  
  String deaccessionNote();
  
  String versionState();

}
