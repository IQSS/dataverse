package edu.harvard.iq.dataverse.workflow.artifacts;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.InputStream;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Startup
@Singleton
public class WorkflowArtifactServiceBean {
    private Map<StorageType, StorageService> services = new HashMap<>();

    private WorkflowArtifactRepository repository;

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowArtifactServiceBean() { }

    @Inject
    public WorkflowArtifactServiceBean(WorkflowArtifactRepository repository) {
        this.repository = repository;
    }

    // -------------------- LOGIC --------------------

    /**
     * Registers service based on service's {@link StorageType}. If there is already a registered service supporting
     * this type of storage, it would be unregistered.
     * @param service service to be registered
     */
    public void register(StorageService service) {
        StorageType storageType = Objects.requireNonNull(service.getStorageType());
        services.put(storageType, service);
    }

    /**
     * Same as {@link WorkflowArtifactServiceBean#saveArtifact(Long, ArtifactData, Clock, StorageType)}
     * but with the last parameter set to {@link StorageType#DATABASE} and clock to {@link Clock#systemDefaultZone()}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowArtifact saveArtifact(Long workflowExecutionId, ArtifactData data) {
        return saveArtifact(workflowExecutionId, data, Clock.systemDefaultZone(), StorageType.DATABASE);
    }

    /**
     * Saves artifact into storage of selected type.
     * Please note that in case of text streams proper encoding is up to caller.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowArtifact saveArtifact(Long workflowExecutionId, ArtifactData data, Clock clock, StorageType storageType) {
        StorageService service = services.get(storageType);
        String location = service.save(data.inputStreamSupplier);

        WorkflowArtifact workflowArtifact = new WorkflowArtifact(null, workflowExecutionId,
                clock.instant(), data.getName(), data.getEncoding(), storageType.name(), location);
        return repository.save(workflowArtifact);
    }

    /**
     * Returns {@link Optional} containing {@link InputStream} of stored data for the given
     * {@link WorkflowArtifact} or empty {@link Optional} if value was not found.
     */
    public Optional<Supplier<InputStream>> readAsStream(WorkflowArtifact artifact) {
        return selectProperService(artifact)
                .readAsStream(artifact.getStorageLocation());
    }

    /**
     * Deletes all stored artifacts for the given {@link WorkflowExecution} id. This means that
     * data would be deleted not only from the <i>workflowartifact</i> table but also from
     * the appropriate storage.
     */
    public void deleteArtifacts(Long workflowExecutionId) {
        List<WorkflowArtifact> oldArtifacts = repository.findAllByWorkflowExecutionId(workflowExecutionId);
        repository.deleteAllByWorkflowExecutionId(workflowExecutionId);
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
        selectProperService(artifact)
                .delete(artifact.getStorageLocation());
    }

    private StorageService selectProperService(WorkflowArtifact artifact) {
        StorageType storageType = StorageType.valueOf(artifact.getStorageType());
        return services.get(storageType);
    }

    // -------------------- INNER CLASSES --------------------

    public static class ArtifactData {
        private final String name;
        private final String encoding;

        private final Supplier<InputStream> inputStreamSupplier;

        public ArtifactData(String name, String encoding, Supplier<InputStream> inputStreamSupplier) {
            this.name = name;
            this.encoding = encoding;
            this.inputStreamSupplier = inputStreamSupplier;
        }

        public String getName() {
            return name;
        }

        public String getEncoding() {
            return encoding;
        }

        public Supplier<InputStream> getInputStreamSupplier() {
            return inputStreamSupplier;
        }
    }
}