/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.validation;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 *
 * @author sarahferry
 */

@Target({FIELD})
@Retention(RUNTIME)
// This is empty by intention - we are just recombining existing bean validation here.
// The class UserNameValidator is just for convenience and historic reasons.
@Constraint(validatedBy = {})

@NotBlank(message = "{user.enterUsername}")
@Size(min = UserNameValidator.MIN_CHARS, max = UserNameValidator.MAX_CHARS, message = "{user.usernameLength}")
@Pattern(regexp = UserNameValidator.USERNAME_PATTERN, message = "{user.illegalCharacters}")

@Documented
public @interface ValidateUserName {
    String message() default "";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
