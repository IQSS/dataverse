/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 *
 * @author skraffmi
 */
public class ConstraintViolationUtil {

    public static String getErrorStringForConstraintViolations(Throwable cause) {

        StringBuilder sb = new StringBuilder();
        if (cause == null) {
            return "";
        }
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (cause instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                    sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                            .append(violation.getPropertyPath()).append(" at ")
                            .append(violation.getLeafBean()).append(" - ")
                            .append(violation.getMessage());
                }
            }
        }
        return sb.toString();
    }

}
