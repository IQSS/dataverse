package edu.harvard.iq.dataverse.api;

import jakarta.ws.rs.ApplicationPath;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("api/v1")
@SecurityScheme(
        securitySchemeName = "DataverseApiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        apiKeyName = "X-Dataverse-key",
        description = "Dataverse API token."
)
public class ApiConfiguration extends ResourceConfig {
   
   public ApiConfiguration() {
       packages("edu.harvard.iq.dataverse.api");
       packages("edu.harvard.iq.dataverse.mydata");
       register(MultiPartFeature.class);
   }
}
