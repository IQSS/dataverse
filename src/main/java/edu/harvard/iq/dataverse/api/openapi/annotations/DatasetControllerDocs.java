package edu.harvard.iq.dataverse.api.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.Operation;

public @interface DatasetControllerDocs {

    @Target({ElementType.METHOD})    
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Dataset")
    @APIResponses(value = {
        @APIResponse(responseCode = "200",
                    description = "Version and build information")
    })  
    @Operation(summary = "Uploads file", 
               description = "Uploads a file for a dataset")
    public @interface AddFileToDataset {}

}
