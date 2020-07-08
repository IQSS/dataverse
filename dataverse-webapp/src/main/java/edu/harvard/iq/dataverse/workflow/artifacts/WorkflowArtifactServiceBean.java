package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Singleton
public class WorkflowArtifactServiceBean {

    private static final Logger log = LoggerFactory.getLogger(WorkflowArtifactServiceBean.class);

    private final WorkflowArtifactRepository repository;
    private final WorkflowArtifactStorage storage;
    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowArtifactServiceBean() {
        this(null, null);
    }

    @Inject
    public WorkflowArtifactServiceBean(WorkflowArtifactRepository repository, WorkflowArtifactStorage storage) {
        this(repository, storage, Clock.systemUTC());
    }

    public WorkflowArtifactServiceBean(WorkflowArtifactRepository repository, WorkflowArtifactStorage storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    // -------------------- LOGIC --------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<WorkflowArtifact> createAll(Long executionId, List<WorkflowArtifactSource> sources) {
        return sources.stream()
                .map(source -> create(executionId, source))
                .collect(toList());
    }

    /**
     * Saves artifact into storage of selected type.
     * Please note that in case of text streams proper encoding is up to caller.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowArtifact create(Long executionId, WorkflowArtifactSource source) {
        try {
            String storageLocation = storage.write(source.getDataSupplier());
            WorkflowArtifact artifact = new WorkflowArtifact(executionId, source, storageLocation, clock);
            return repository.save(artifact);
        } catch (IOException e) {
            log.warn("Failed storing workflow execution " + executionId + " artifact " + source.getName(), e);
            throw new RuntimeException(e);
        }
    }

    public List<WorkflowArtifact> findAll(Long workflowExecutionId) {
        return repository.findByWorkflowExecutionId(workflowExecutionId);
    }

    /**
     * Returns {@link Optional} containing {@link InputStream} of stored data for the given
     * {@link WorkflowArtifact} or empty {@link Optional} if value was not found.
     */
    public Optional<InputSupplier<InputStream>> readAsStream(String storageLocation) {
        return storage.read(storageLocation);
    }
}
