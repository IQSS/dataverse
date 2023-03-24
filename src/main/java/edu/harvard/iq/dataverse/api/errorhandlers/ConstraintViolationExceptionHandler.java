package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Set;

@Provider
public class ConstraintViolationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        
        // This hack and code duplication is necessary as there is no way to make createResponse(Set<ConstraintViolation<T>>)
        // also accept Set<ConstraintViolation<?>>. So we split by creating the JsonArray before the Response.
        exception.getConstraintViolations().forEach(violation -> builder.add(
            Json.createObjectBuilder()
                .add("path", violation.getPropertyPath().toString())
                .add("message", violation.getMessage())
                .add("invalidValue", violation.getInvalidValue() == null ? "null" : violation.getInvalidValue().toString())));
        
        return createResponse(builder.build());
    }
    
    /**
     * Create a nice JSON based response from a set of constraint violations.
     * @param violations The violations (will be transformed to JSON objects)
     * @return A {@link Response} for JAX-RS, containing a JSON based description of the problems
     */
    public static <T> Response createResponse(Set<ConstraintViolation<T>> violations) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        
        violations.forEach(violation -> builder.add(
            Json.createObjectBuilder()
                .add("path", violation.getPropertyPath().toString())
                .add("message", violation.getMessage())
                .add("invalidValue", violation.getInvalidValue() == null ? "null" : violation.getInvalidValue().toString())));
        
        return createResponse(builder.build());
    }
    
    private static Response createResponse(JsonArray violations) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity( Json.createObjectBuilder()
                .add("status", "ERROR")
                .add("code", Response.Status.BAD_REQUEST.getStatusCode())
                .add("message", "JPA validation constraints failed persistence. See list of violations for details.")
                .add("violations", violations)
                .build())
            .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}