package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatabaseWorkflowArtifactStorageIT extends WebappArquillianDeployment {

    private static final byte[] TEST_DATA = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DatabaseWorkflowArtifactStorage storageService;

    @Test
    public void shouldStoreAndRetrieveArtifactData() throws IOException {
        // given & when
        String location = storageService.write(() -> new ByteArrayInputStream(TEST_DATA));
        byte[] read = IOUtils.toByteArray(storageService.read(location)
                .orElseThrow(IllegalStateException::new)
                .getInput());

        // then
        assertThat(read).isEqualTo(read);
    }

    @Test
    public void shouldBeAbleToDeleteStoredData() throws IOException {
        // given & when
        String location = storageService.write(() -> new ByteArrayInputStream(TEST_DATA));
        storageService.delete(location);
        Optional<InputSupplier<InputStream>> read = storageService.read(location);

        // then
        assertThat(read.isPresent()).isFalse();
    }
}