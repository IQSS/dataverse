package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.workflow.internalspi.InternalWorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static edu.harvard.iq.dataverse.workflow.internalspi.InternalWorkflowStepSPI.INTERNAL_PROVIDER_ID;

/**
 * Registry of all available {@link WorkflowStepSPI}'s.
 * <p>
 * Call {@link #register(String, WorkflowStepSPI)} method to add more SPI's to the registry.
 */
@Startup
@Singleton
public class WorkflowStepRegistry {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStepRegistry.class);

    private final Map<String, WorkflowStepSPI> providers = new ConcurrentHashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    @PostConstruct
    public void init() {
        register(INTERNAL_PROVIDER_ID, new InternalWorkflowStepSPI());
    }

    // -------------------- LOGIC --------------------

    public void register(String providerId, WorkflowStepSPI provider) {
        Objects.requireNonNull(providerId, "Provider ID is required");
        Objects.requireNonNull(provider, "Provider is required");

        WorkflowStepSPI previous = providers.putIfAbsent(providerId, provider);
        if (previous == null) {
            log.info("Registered {} workflow step provider of {}", providerId, provider.getClass().getCanonicalName());
        } else {
            throw new IllegalArgumentException("Duplicate provider ID for " + provider.getClass().getCanonicalName() +
                    " - ID was already assigned to " + previous.getClass().getCanonicalName());
        }
    }

    public WorkflowStepSPI getProvider(String providerId) {
        if (!providers.containsKey(providerId)) {
            throw new IllegalArgumentException("Unknown provider with ID '" + providerId + "'");
        }
        return providers.get(providerId);
    }

    public WorkflowStep getStep(String providerId, String stepType, Map<String, String> parameters) {
        return getProvider(providerId)
                .getStep(stepType, new WorkflowStepParams(parameters));
    }
}
