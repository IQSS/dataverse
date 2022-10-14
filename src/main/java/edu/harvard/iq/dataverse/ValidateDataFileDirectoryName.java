/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 *
 * @author skraffmi
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {FileDirectoryNameValidator.class})
@Documented
public @interface ValidateDataFileDirectoryName {
    
    String message() default "Failed Validation for Validate Data File Directory Name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    
}
