package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;

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
    public List<WorkflowArtifact> saveAll(Long executionId, WorkflowArtifactsSupplier supplier) {
        return supplier.getArtifacts().stream()
                .map(source -> saveArtifact(executionId, source))
                .collect(toList());
    }
    /**
     * Saves artifact into storage of selected type.
     * Please note that in case of text streams proper encoding is up to caller.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowArtifact saveArtifact(Long executionId, WorkflowArtifactSource source) {
        String location;
        try {
            location = storage.write(source.getDataSupplier());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorkflowArtifact workflowArtifact = new WorkflowArtifact(executionId,
                source.getName(), source.getEncoding(), storage.getType().name(), location, clock);
        return repository.save(workflowArtifact);
    }

    /**
     * Returns {@link Optional} containing {@link InputStream} of stored data for the given
     * {@link WorkflowArtifact} or empty {@link Optional} if value was not found.
     */
    public Optional<InputSupplier<InputStream>> readAsStream(WorkflowArtifact artifact) {
        return storage.read(artifact.getStorageLocation());
    }

    /**
     * Deletes all stored artifacts for the given {@link WorkflowExecution} id. This means that
     * data would be deleted not only from the <i>workflowartifact</i> table but also from
     * the appropriate storage.
     */
    public void deleteArtifacts(Long workflowExecutionId) {
        List<WorkflowArtifact> oldArtifacts = repository.findByWorkflowExecutionId(workflowExecutionId);
        repository.deleteByWorkflowExecutionId(workflowExecutionId);
        oldArtifacts.forEach(this::deleteFromStorage);
    }

    /**
     * Deletes the given {@link WorkflowArtifact} from <i>workflowartifact</i> table
     * and from the storage.
     */
    public void deleteArtifact(WorkflowArtifact artifact) {
        repository.delete(artifact);
        deleteFromStorage(artifact);
    }

    // -------------------- PRIVATE --------------------

    private void deleteFromStorage(WorkflowArtifact artifact) {
        try {
            storage.delete(artifact.getStorageLocation());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
