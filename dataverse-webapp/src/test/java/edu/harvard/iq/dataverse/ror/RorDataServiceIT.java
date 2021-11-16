package edu.harvard.iq.dataverse.ror;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorDataRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.DISABLED)
public class RorDataServiceIT extends WebappArquillianDeployment {

    @Inject
    private RorDataService rorDataService;

    @Inject
    private RorDataRepository rorDataRepository;

    @Inject
    private DataverseSession session;

    private File rorData;

    @BeforeEach
    public void beforeClass() throws Exception {
        rorDataRepository.truncateAll();
    }

    @AfterEach
    void afterEach() throws IOException {
        Files.deleteIfExists(rorData.toPath());
    }

    @Test
    public void refreshRorData() throws IOException {
        // given
        final File file = copyRorDataFromClasspath();
        final AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setSuperuser(true);
        session.setUser(authenticatedUser);

        // when
        final RorDataService.UpdateResult niceFile = rorDataService.refreshRorData(file, FormDataContentDisposition
                .name("niceFile")
                .fileName(".json")
                .build());

        // then
        Assertions.assertThat(niceFile.getTotal()).isEqualTo(2);
        Assertions.assertThat(rorDataRepository.findAll())
                .extracting(RorData::getRorId)
                .containsExactlyInAnyOrder("011xxxx11", "013cjyk83");
    }

    private File copyRorDataFromClasspath() throws IOException {
        final File file = Paths.get("src", "test", "resources", "json", "ror", "rorData.json").toFile();
        final Path tempFile = Files.createTempFile("", "");
        Files.copy(file.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        rorData = tempFile.toFile();
        return rorData;
    }
}
