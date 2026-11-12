package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;
import edu.harvard.iq.dataverse.license.License;
import static org.junit.jupiter.api.Assertions.*;

public class TermsOfUseAndAccessTest {

    @Test
    public void testLicenseAndTermsMutualExclusivity() {
        License license = new License();
        license.setName("CC0");

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();

        // 1. Setting license should clear terms
        terms.setTermsOfUse("Some terms");
        assertEquals("Some terms", terms.getTermsOfUse());
        assertNull(terms.getLicense());

        terms.setLicense(license);
        assertNull(terms.getTermsOfUse());
        assertEquals(license, terms.getLicense());

        // 2. Setting terms should clear license
        terms.setTermsOfUse("New terms");
        assertNull(terms.getLicense());
        assertEquals("New terms", terms.getTermsOfUse());

        // 3. Test other fields clear license too
        terms.setLicense(license);
        terms.setConfidentialityDeclaration("Confidential");
        assertNull(terms.getLicense());
        assertEquals("Confidential", terms.getConfidentialityDeclaration());
    }

    @Test
    public void testCopyTermsOfUseAndAccess() {
        License license = new License();
        license.setName("CC0");

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(license);

        TermsOfUseAndAccess copy = terms.copyTermsOfUseAndAccess();
        assertEquals(license, copy.getLicense());
        assertNull(copy.getTermsOfUse());

        terms.setTermsOfUse("Some terms");
        TermsOfUseAndAccess copy2 = terms.copyTermsOfUseAndAccess();
        assertNull(copy2.getLicense());
        assertEquals("Some terms", copy2.getTermsOfUse());
    }
}
