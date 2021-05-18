package edu.harvard.iq.dataverse.api.annotations;

import edu.harvard.iq.dataverse.api.filters.ApiReadonlyInstanceFilter;

import javax.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks api endpoints that
 * performs some write operations on database or storage.
 * <br/>
 * Information about write operations is used to block
 * those endpoints in case Dataverse installation operates
 * in readonly mode.
 * 
 * @author madryk
 * @see ApiReadonlyInstanceFilter
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ApiWriteOperation {

}
