package edu.harvard.iq.dataverse.workflow.artifacts;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.workflow.artifacts.WorkflowArtifactServiceBean.ArtifactData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Vetoed;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkflowArtifactServiceBeanTest {

    private static final String ENCODING = "BINARY";
    private static final String NAME = "TEST";

    private static final Supplier<InputStream> TEST_DATA_SUPPLIER =
            () -> new ByteArrayInputStream(new byte[] { 0, 1, 2, 3 });

    private WorkflowArtifactServiceBean serviceBean;

    private WorkflowArtifactRepository repository;

    private TestStorageService storageService;


    @BeforeEach
    public void setUp() {
        repository = new Repository();
        serviceBean = new WorkflowArtifactServiceBean(repository);

        storageService = new TestStorageService();
        serviceBean.register(storageService);
    }

    @Test
    @DisplayName("Should save artifact data and metadata")
    public void shouldSaveDataAndMetadata() {
        // given
        ArtifactData data = new ArtifactData(NAME, ENCODING, TEST_DATA_SUPPLIER);

        // when
        WorkflowArtifact artifact =
                serviceBean.saveArtifact(1L,  data, Clock.systemUTC(), storageService.getStorageType());

        // then
        assertThat(artifact.getId()).isNotNull();
        assertThat(artifact.getCreatedAt()).isNotNull();
        assertThat(artifact.getStorageLocation()).isNotNull();
        assertThat(artifact.getStorageType()).isEqualTo(storageService.getStorageType().name());
        assertThat(artifact.getName()).isEqualTo(NAME);
        assertThat(artifact.getEncoding()).isEqualTo(ENCODING);

        assertThat(storageService.getNumberOfStoredObjects()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should be able to retrieve stored artifact data")
    public void shouldRetrieveStoredData() throws IOException {
        // given
        ArtifactData data = new ArtifactData(NAME, ENCODING, TEST_DATA_SUPPLIER);

        // when
        WorkflowArtifact artifact = serviceBean.saveArtifact(1L, data);
        Optional<Supplier<InputStream>> streamSupplier = serviceBean.readAsStream(artifact);

        // then
        assertThat(streamSupplier.isPresent()).isTrue();

        assertThat(storageService.getReadCounter()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should delete stored artifacts for the given dataset version")
    public void shouldDeleteArtifactsForGivenDatasetVersion() {
        // given
        final int size = 10;
        ArtifactData data = new ArtifactData(NAME, ENCODING, TEST_DATA_SUPPLIER);

        // when
        IntStream.rangeClosed(1, size).forEach(i -> serviceBean.saveArtifact(1L, data));
        List<WorkflowArtifact> before = repository.findAllByWorkflowExecutionId(1L);
        serviceBean.deleteArtifacts(1L);
        List<WorkflowArtifact> after = repository.findAllByWorkflowExecutionId(1L);

        // then
        assertThat(before.size()).isEqualTo(size);
        assertThat(after).isEmpty();

        assertThat(storageService.getNumberOfStoredObjects()).isEqualTo(0);
    }

    // -------------------- INNER CLASSES --------------------

    private static class TestStorageService implements StorageService {
        private Map<String, Supplier<InputStream>> storage = new HashMap<>();

        private int readCounter = 0;

        @Override
        public StorageType getStorageType() {
            return StorageType.DATABASE;
        }

        @Override
        public String save(Supplier<InputStream> inputStreamSupplier) {
            String location = UUID.randomUUID().toString();
            storage.put(location, inputStreamSupplier);
            return location;
        }

        @Override
        public Optional<Supplier<InputStream>> readAsStream(String location) {
            readCounter++;
            Supplier<InputStream> inputStreamSupplier = storage.get(location);
            return inputStreamSupplier != null
                    ? Optional.of(inputStreamSupplier)
                    : Optional.empty();
        }

        @Override
        public void delete(String location) {
            storage.remove(location);
        }

        public int getNumberOfStoredObjects() {
            return storage.size();
        }

        public int getReadCounter() {
            return readCounter;
        }
    }

    @Vetoed
    private static class Repository extends WorkflowArtifactRepository {
        private Map<Long, WorkflowArtifact> storage = new HashMap<>();
        long counter = 0L;

        public Repository() {
            super();
        }

        @Override
        public List<WorkflowArtifact> findAllByWorkflowExecutionId(Long workflowExecutionId) {
            return storage.values().stream()
                    .filter(v -> workflowExecutionId.equals(v.getWorkflowExecutionId()))
                    .collect(Collectors.toList());
        }

        @Override
        public int deleteAllByWorkflowExecutionId(Long workflowExecutionId) {
            List<WorkflowArtifact> found = findAllByWorkflowExecutionId(workflowExecutionId);
            storage.values().removeAll(found);
            return found.size();
        }

        @Override
        public WorkflowArtifact save(WorkflowArtifact entity) {
            long id = entity.getId() != null ? entity.getId() : counter++;
            WorkflowArtifact saved = new WorkflowArtifact(id, entity.getWorkflowExecutionId(), entity.getCreatedAt(),
                    entity.getName(), entity.getEncoding(), entity.getStorageType(), entity.getStorageLocation());
            storage.put(id, saved);
            return saved;
        }

        @Override
        public void delete(WorkflowArtifact entity) {
            storage.remove(entity.getId());
        }
    }
}