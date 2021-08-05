package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
public class InitialTermsOfUseFactoryTest {

    @InjectMocks
    private TermsOfUseFactory termsOfUseFactory;

    @Mock
    private LicenseRepository licenseRepository;


    // -------------------- TESTS --------------------

    @Test
    public void createTermsOfUse() {
        // given
        License license = mock(License.class);
        when(licenseRepository.findFirstActive()).thenReturn(license);

        // when
        FileTermsOfUse termsOfUse = termsOfUseFactory.createTermsOfUse();

        // then
        assertThat(termsOfUse)
            .extracting(FileTermsOfUse::getTermsOfUseType, FileTermsOfUse::getLicense)
            .containsExactly(TermsOfUseType.LICENSE_BASED, license);
    }

    @Test
    public void createUnknownTermsOfUse() {
        //when
        final FileTermsOfUse unknownTermsOfUse = termsOfUseFactory.createUnknownTermsOfUse();

        //then
        Assertions.assertThat(unknownTermsOfUse.getTermsOfUseType()).isEqualTo(TermsOfUseType.TERMS_UNKNOWN);
    }

    @Test
    void createTermsOfUseFromCC0License() {
        //given
        final License license = new License();
        license.setName("CC0 Creative Commons Zero 1.0 Waiver");

        //when
        Mockito.when(licenseRepository.getById(1L)).thenReturn(license);
        final FileTermsOfUse termsOfUseFromCC0License = termsOfUseFactory.createTermsOfUseFromCC0License();

        //then
        Assertions.assertThat(termsOfUseFromCC0License).extracting(fileTermsOfUse -> fileTermsOfUse.getLicense().getName())
                  .isEqualTo(license.getName());

    }

    @Test
    void createTermsOfUseWithExistingLicense() {
        //given
        String licenseToFind = "license";
        final License license = new License();

        //when
        Mockito.when(licenseRepository.findLicenseByName(licenseToFind)).thenReturn(Optional.of(license));
        final Optional<FileTermsOfUse> termsOfUseWithExistingLicense = termsOfUseFactory
                .createTermsOfUseWithExistingLicense(licenseToFind);

        //then
        assertThat(termsOfUseWithExistingLicense)
            .isPresent().get()
            .extracting(fileTermsOfUse -> fileTermsOfUse.getLicense())
            .isSameAs(license);

    }
}
