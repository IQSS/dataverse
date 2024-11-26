package edu.harvard.iq.dataverse.api.openapi;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(
    info = @org.eclipse.microprofile.openapi.annotations.info.Info
    (
        title = "Dataverse Hub API", 
        version = "1.0", 
        description = "API for Dataverse Hub",
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0"),
        contact = @Contact(name = "Dataverse Hub", email = "support@dataverse.harvard.edu", url = "https://dataverse.org")     
    ),
    security = @SecurityRequirement(name = "api_key"),
    tags = {@Tag(name = "Info", description = "General information about the Dataverse installation."),
            @Tag(name = "Dataset", description = "Operation related to datasets")}
    
)
@SecuritySchemes({   
    @SecurityScheme(
        securitySchemeName = "api_key",
        type = SecuritySchemeType.APIKEY,
        scheme = "api_key",
        description = "API key for accessing the API")

})
public class JXRSApp extends Application {
}
