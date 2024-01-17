package edu.harvard.iq.dataverse.api.auth;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Guillermo Portas
 * Annotation intended to be placed on any API method that requires user authentication.
 * Marks the API methods whose related requests should be filtered by {@link edu.harvard.iq.dataverse.api.auth.AuthFilter}.
 */
@NameBinding
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AuthRequired {
}
