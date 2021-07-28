package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseDAO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private LicenseDAO licenseDao;


    // -------------------- TESTS --------------------

    @Test
    public void createTermsOfUse() {
        // given
        License license = mock(License.class);
        when(licenseDao.findFirstActive()).thenReturn(license);

        // when
        FileTermsOfUse termsOfUse = termsOfUseFactory.createTermsOfUse();

        // then
        assertEquals(TermsOfUseType.LICENSE_BASED, termsOfUse.getTermsOfUseType());
        assertEquals(license, termsOfUse.getLicense());
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
        Mockito.when(licenseDao.find(1)).thenReturn(license);
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
        Mockito.when(licenseDao.findLicenseByName(licenseToFind)).thenReturn(license);
        final FileTermsOfUse termsOfUseWithExistingLicense = termsOfUseFactory.createTermsOfUseWithExistingLicense(licenseToFind);

        //then
        Assertions.assertThat(termsOfUseWithExistingLicense).extracting(fileTermsOfUse -> fileTermsOfUse.getLicense().getName())
                  .isEqualTo(license.getName());

    }
}
