package edu.harvard.iq.dataverse.persistence.datafile.license;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Test;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author madryk
 */
public class LicenseRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private LicenseRepository licenseRepository;


    //-------------------- TESTS --------------------

    @Test
    public void findFirstActive() {
        // when
        License license = licenseRepository.findFirstActive();

        // then
        assertThat(license)
            .extracting(License::getId, License::getName)
            .containsExactly(1L, "CC0 Creative Commons Zero 1.0 Waiver");
    }

    @Test
    public void findAllOrderedByPosition() {
        // given
        License licenseToMove = licenseRepository.getById(2L);
        licenseToMove.setPosition(40L);
        licenseRepository.save(licenseToMove);
        
        // when
        List<License> licenses = licenseRepository.findAllOrderedByPosition();

        // then
        assertThat(licenses).hasSize(14)
            .extracting(License::getName)
            .containsExactly(
                "CC0 Creative Commons Zero 1.0 Waiver",
                "CC BY-SA Creative Commons Attribution - ShareAlike License 4.0",
                "CC BY-NC Creative Commons Attribution - NonCommercial License 4.0",
                "CC BY-ND Creative Commons Attribution - NoDerivs License 4.0",
                "CC BY-NC-SA Creative Commons Attribution - NonCommercial - ShareAlike License 4.0",
                "CC BY-NC-ND Creative Commons Attribution - NonCommercial - NoDerivs License 4.0",
                "Apache Software License 2.0",
                "BSD 3 – Clause “New” or “Revised” License",
                "GNU General Public License 3.0",
                "GNU Lesser General Public License 3.0",
                "GNU Affero General Public License version 3",
                "MIT License",
                "Open Data Commons Open Database License 1.0",
                "CC BY Creative Commons Attribution License 4.0");
    }

    @Test
    public void findLicenseByName() {
        //given
        String licenseName = "CC0 Creative Commons Zero 1.0 Waiver";

        //when
        final Optional<License> licenseFound = licenseRepository.findLicenseByName(licenseName);

        //then
        assertThat(licenseFound).isPresent()
            .get()
            .extracting(License::getName).isEqualTo(licenseName);
    }

    @Test
    public void findLicenseByName_not_existing() {
        //when
        final Optional<License> licenseFound = licenseRepository.findLicenseByName("not exists");

        //then
        assertThat(licenseFound).isNotPresent();
    }
}
