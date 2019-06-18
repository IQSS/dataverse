package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.arquillian.DataverseArquillian;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.ArquillianDeployment;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author madryk
 */
@RunWith(DataverseArquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class LicenseDAOIT extends ArquillianDeployment {

    @Inject
    private LicenseDAO licenseDao;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;


    //-------------------- TESTS --------------------

    @Test
    public void find() {
        // when
        License license = licenseDao.find(2L);

        // then
        assertEquals(Long.valueOf(2L), license.getId());
        assertEquals("CC BY Creative Commons Attribution License 4.0", license.getName());
        assertEquals("https://creativecommons.org/licenses/by/4.0/legalcode", license.getUrl());
        assertFalse(license.isActive());

        assertEquals(2, license.getLocalizedNames().size());
        assertEquals("CC BY - Creative Commons Attribution 4.0", license.getLocalizedName(Locale.ENGLISH));
        assertEquals("CC BY - Creative Commons Uznanie Autorstwa 4.0", license.getLocalizedName(new Locale("pl")));

        assertEquals("image/png", license.getIcon().getContentType());
        assertEquals(2601, license.getIcon().getContent().length);
    }

    @Test
    public void findFirstActive() {
        // when
        License license = licenseDao.findFirstActive();

        // then
        assertEquals(Long.valueOf(1L), license.getId());
        assertEquals("CC0 Creative Commons Zero 1.0 Waiver", license.getName());
    }

    @Test
    public void findAll() {
        // when
        List<License> licenses = licenseDao.findAll();

        // then
        assertEquals(14, licenses.size());

        List<String> licenseNames = licenses.stream().map(l -> l.getName()).collect(toList());
        assertThat(licenseNames, contains(
                "CC0 Creative Commons Zero 1.0 Waiver",
                "CC BY Creative Commons Attribution License 4.0",
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
                "Open Data Commons Open Database License 1.0"
        ));
    }
}
