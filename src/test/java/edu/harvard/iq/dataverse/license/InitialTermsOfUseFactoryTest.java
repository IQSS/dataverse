package edu.harvard.iq.dataverse.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.harvard.iq.dataverse.license.FileTermsOfUse.TermsOfUseType;

/**
 * @author madryk
 */
@RunWith(MockitoJUnitRunner.class)
public class InitialTermsOfUseFactoryTest {

    @InjectMocks
    private InitialTermsOfUseFactory termsOfUseFactory;
    
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
}
