package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;
import edu.harvard.iq.dataverse.validation.ValidationUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Set;

@Provider
public class ConstraintViolationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {
    
    public record ValidationError(String path, String message) {}
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return JsonResponseBuilder.error(Response.Status.BAD_REQUEST)
            .message("Request validation failed. See list of violations for details.")
            .violations(toViolations(exception.getConstraintViolations()))
            .build();
    }
    
    private List<JsonResponseBuilder.Violation> toViolations(Set<ConstraintViolation<?>> violations) {
        return violations.stream()
            .map(cv -> new JsonResponseBuilder.Violation(
                ValidationUtil.propertyPath(cv),
                cv.getMessage())
            )
            .toList();
    }
}