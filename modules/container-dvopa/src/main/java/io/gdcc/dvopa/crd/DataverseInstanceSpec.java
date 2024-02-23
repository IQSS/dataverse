package io.gdcc.dvopa.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.Max;
import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.SecurityContext;

import java.util.List;
import java.util.Map;

public record DataverseInstanceSpec(
    @Required
    String version,
    
    @Min(1)
    @Max(10)
    @Default("1")
    int replicas,
    
    @Required
    Map<String,String> jvmOptions,
    
    Map<String,String> dbOptions,
    
    Map<String,String> environmentVariables,
    
    Map<String,String> jvmArgs,
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Container> initContainers,
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Container> sidecarContainers,
    
    SecurityContext securityContext
) {}
