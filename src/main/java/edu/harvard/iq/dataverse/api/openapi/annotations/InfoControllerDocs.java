package edu.harvard.iq.dataverse.api.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.Operation;
//import org.eclipse.microprofile.openapi.annotations.Operation;


public @interface InfoControllerDocs {

    @Target({ElementType.METHOD})    
    @Retention(RetentionPolicy.RUNTIME)
    @APIResponses(value = {
        @APIResponse(responseCode = "200",
                    description = "Version and build information",
                    content = @Content(mediaType = "application/json"))
    })      
    @Operation(summary = "Version information", 
                description = "Provides the version information for the Dataverse installation.") 
    @Tags(value = {
        @Tag(name = "Info")
    })
    public @interface GetInfo{}

}
