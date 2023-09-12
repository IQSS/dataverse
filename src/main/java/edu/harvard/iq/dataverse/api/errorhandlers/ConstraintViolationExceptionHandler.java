package edu.harvard.iq.dataverse.api.errorhandlers;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {
    
    public class ValidationError {
        private String path;
        private String message;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        
        List<ValidationError> errors = exception.getConstraintViolations().stream()
            .map(this::toValidationError)
            .collect(Collectors.toList());
        
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity( Json.createObjectBuilder()
                           .add("status", "ERROR")
                           .add("code", Response.Status.BAD_REQUEST.getStatusCode())
                           .add("message", "JPA validation constraints failed persistence. See list of violations for details.")
                           .add("violations", toJsonArray(errors))
                           .build())
                       .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    
    private ValidationError toValidationError(ConstraintViolation constraintViolation) {
        ValidationError error = new ValidationError();
        error.setPath(constraintViolation.getPropertyPath().toString());
        error.setMessage(constraintViolation.getMessage());
        return error;
    }
    
    private JsonArray toJsonArray(List<ValidationError> list) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        list.stream()
            .forEach(error -> builder.add(
                Json.createObjectBuilder()
                    .add("path", error.getPath())
                    .add("message", error.getMessage())));
        return builder.build();
    }
}