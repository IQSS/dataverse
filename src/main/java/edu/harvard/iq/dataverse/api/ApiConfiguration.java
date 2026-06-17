package edu.harvard.iq.dataverse.api;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("api/v1")
public class ApiConfiguration extends ResourceConfig {
   
   public ApiConfiguration() {
       packages("edu.harvard.iq.dataverse.api");
       packages("edu.harvard.iq.dataverse.mydata");
       register(MultiPartFeature.class);
   }
}
