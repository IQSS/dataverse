package edu.harvard.iq.dataverse.license;

import javax.ejb.Stateless;
import javax.inject.Inject;


/**
 * Factory of {@link FileTermsOfUse} objects.
 * 
 * @author madryk
 */
@Stateless
public class InitialTermsOfUseFactory {

    @Inject
    private LicenseDAO licenseDao;
    
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns new instance of license based {@link FileTermsOfUse}
     * with license set to first active one.
     */
    public FileTermsOfUse createTermsOfUse() {
        
        License defaultLicense = licenseDao.findFirstActive();
        
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setLicense(defaultLicense);
        
        return termsOfUse;
    }
}
