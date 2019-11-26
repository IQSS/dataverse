package edu.harvard.iq.dataverse.dataverse.themewidget;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ThemeWidgetServiceIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private ThemeWidgetService themeWidgetService;
    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }


    @Test
    public void shouldSaveOrUpdateUploadedTheme() {
        // given
        Dataverse dataverse = dataverseDao.findByAlias("ownmetadatablocks");
        File file = new File (getClass().getClassLoader()
                .getResource("images/banner.png").getFile());

        DataverseTheme dvTheme = dataverse.getDataverseTheme();
        dvTheme.setLogo("banner.png");
        dvTheme.setLogoFormat(DataverseTheme.ImageFormat.RECTANGLE);
        dvTheme.setLinkUrl("http://icm.edu.pl");
        dvTheme.setLogoAlignment(DataverseTheme.Alignment.RIGHT);
        dvTheme.setBackgroundColor("FFFFAA");
        dvTheme.setLogoBackgroundColor("FFFFAA");
        dvTheme.setLinkColor("FFFFAA");
        dvTheme.setTextColor("FFFFAA");
        dvTheme.setTagline("testTagline");

        // when
        themeWidgetService.saveOrUpdateUploadedTheme(dataverse, file);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("ownmetadatablocks");
        DataverseTheme dbDvTheme = dbDataverse.getDataverseTheme();

        assertEquals("Root", dbDataverse.getThemeRootDataverseName());
        assertEquals("banner.png", dbDvTheme.getLogo());
        assertEquals(DataverseTheme.ImageFormat.RECTANGLE, dbDvTheme.getLogoFormat());
        assertEquals(DataverseTheme.Alignment.RIGHT, dbDvTheme.getLogoAlignment());
        assertEquals("http://icm.edu.pl", dbDvTheme.getLinkUrl());
        assertEquals("FFFFAA", dbDvTheme.getBackgroundColor());
        assertEquals("FFFFAA", dbDvTheme.getLogoBackgroundColor());
        assertEquals("FFFFAA", dbDvTheme.getLinkColor());
        assertEquals("FFFFAA", dbDvTheme.getTextColor());
        assertEquals("testTagline", dbDvTheme.getTagline());

        Path logoPath = Paths.get("../docroot/logos" ,dbDataverse.getId().toString(), dbDataverse.getDataverseTheme().getLogo());

        assertTrue(Files.exists(logoPath));
        assertEquals("banner.png", logoPath.toFile().getName());
    }

    @Test
    public void shouldInheritThemeFromRoot() {
        // given
        Dataverse dataverse = dataverseDao.findByAlias("unreleased");
        dataverse.setThemeRoot(false);
        em.persist(dataverse);


        // when
        themeWidgetService.inheritThemeFromRoot(dataverse);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("unreleased");
        assertEquals("Root", dbDataverse.getThemeRootDataverseName());
        assertEquals(dataverseDao.findByAlias("root").getDataverseTheme(), dbDataverse.getDataverseTheme());

        Path logoPath = Paths.get("../docroot/logos",dbDataverse.getId().toString(), dbDataverse.getDataverseTheme().getLogo());

        assertFalse(logoPath.toFile().exists());
    }
}
