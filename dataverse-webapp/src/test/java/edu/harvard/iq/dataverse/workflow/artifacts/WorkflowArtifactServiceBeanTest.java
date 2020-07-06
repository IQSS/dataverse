package edu.harvard.iq.dataverse.workflow.artifacts;

import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.test.WithTestClock;
import edu.harvard.iq.dataverse.workflow.artifacts.WorkflowArtifactServiceBean.ArtifactData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class WorkflowArtifactServiceBeanTest implements WithTestClock {

    static final String ENCODING = "BINARY";
    static final String NAME = "TEST";
    static final Supplier<InputStream> DATA_SUPPLIER = () -> mock(InputStream.class);

    StubJpaPersistence persistence = new StubJpaPersistence();
    WorkflowArtifactRepository repository = persistence.stub(WorkflowArtifactRepository.class);
    StorageService storageService = mock(StorageService.class);

    WorkflowArtifactServiceBean serviceBean = new WorkflowArtifactServiceBean(repository, clock);

    ArtifactData data = new ArtifactData(NAME, ENCODING, DATA_SUPPLIER);

    @BeforeEach
    public void setUp() {
        doReturn(StorageType.DATABASE)
                .when(storageService).getStorageType();
        serviceBean.register(storageService);
    }

    @Test
    @DisplayName("Should save artifact data and metadata")
    public void shouldSaveDataAndMetadata() {
        // given
        doReturn("testLocation")
                .when(storageService).save(any(Supplier.class));

        // when
        WorkflowArtifact artifact =
                serviceBean.saveArtifact(1L,  data, StorageType.DATABASE);

        // then
        assertThat(artifact.getId()).isNotNull();
        assertThat(artifact.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(artifact.getStorageLocation()).isEqualTo("testLocation");
        assertThat(artifact.getStorageType()).isEqualTo(StorageType.DATABASE.name());
        assertThat(artifact.getName()).isEqualTo(NAME);
        assertThat(artifact.getEncoding()).isEqualTo(ENCODING);
    }

    @Test
    @DisplayName("Should be able to retrieve stored artifact data")
    public void shouldRetrieveStoredData() {
        // given
        WorkflowArtifact artifact = new WorkflowArtifact(1L, NAME, ENCODING,
                StorageType.DATABASE.name(), "testLocation", clock);
        doReturn(Optional.of(DATA_SUPPLIER))
                .when(storageService).readAsStream("testLocation");

        // when
        Optional<Supplier<InputStream>> streamSupplier = serviceBean.readAsStream(artifact);

        // then
        assertThat(streamSupplier.isPresent()).isTrue();
    }
}
