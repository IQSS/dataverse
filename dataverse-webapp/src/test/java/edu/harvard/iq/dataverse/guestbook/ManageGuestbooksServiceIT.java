package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ManageGuestbooksServiceIT extends WebappArquillianDeployment {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    private ManageGuestbooksService manageGuestbooksService;

    @Inject
    private GuestbookServiceBean guestbookService;

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
    public void shouldDeleteGuestbook() {
        // given
        Dataverse dataverse = dataverseDao.findByAlias("ownmetadatablocks");
        long guestbookId = dataverse.getGuestbooks().get(0).getId();

        // when
        manageGuestbooksService.deleteGuestbook(guestbookId);

        // then
        Assert.assertEquals(0, dataverseDao.findByAlias("ownmetadatablocks").getGuestbooks().size());
        Assert.assertNull(guestbookService.find(guestbookId));
    }


    @Test
    public void shouldEnableGuestbook() {
        // given
        long guestbookId = 2L;
        Guestbook guestbook = guestbookService.find(guestbookId);
        guestbook.setEnabled(false);
        em.persist(guestbook);

        // when
        manageGuestbooksService.enableGuestbook(guestbookId);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("ownmetadatablocks");
        Assert.assertEquals(1, dbDataverse.getGuestbooks().size());
        Assert.assertTrue(dbDataverse.getGuestbooks().get(0).isEnabled());
    }

    @Test
    public void shouldDisableGuestbook() {
        // given
        long guestbookId = 2L;
        Guestbook guestbook = guestbookService.find(guestbookId);
        guestbook.setEnabled(true);
        em.persist(guestbook);

        // when
        manageGuestbooksService.disableGuestbook(guestbookId);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("ownmetadatablocks");
        Assert.assertEquals(1, dbDataverse.getGuestbooks().size());
        Assert.assertFalse(dbDataverse.getGuestbooks().get(0).isEnabled());
    }

    @Test
    public void shouldUpdateAllowGuestbooksFromRootStatus_ALLOWED() {
        // given
        Dataverse dataverse = createTestDataverse();

        dataverse.setGuestbookRoot(false);
        em.persist(dataverse);
        em.flush();

        // when
        manageGuestbooksService.updateAllowGuestbooksFromRootStatus(dataverse.getId(), true);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("testDvAlias");
        Assert.assertTrue(dbDataverse.isGuestbookRoot());
    }

    @Test
    public void shouldUpdateAllowGuestbooksFromRootStatus_NOT_ALLOWED() {
        // given
        Dataverse dataverse = createTestDataverse();

        dataverse.setGuestbookRoot(true);
        em.persist(dataverse);
        em.flush();

        // when
        manageGuestbooksService.updateAllowGuestbooksFromRootStatus(dataverse.getId(), false);

        // then
        Dataverse dbDataverse = dataverseDao.findByAlias("testDvAlias");
        Assert.assertFalse(dbDataverse.isGuestbookRoot());
    }


    // -------------------- PRIVATE ---------------------
    private Dataverse createTestDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setName("testDv");
        dataverse.setAlias("testDvAlias");
        dataverse.setDataverseType(Dataverse.DataverseType.LABORATORY);
        dataverse.setDataverseContacts(dataverseDao.findByAlias("ownmetadatablocks").getDataverseContacts());
        dataverse.setOwner(dataverseDao.findByAlias("ownmetadatablocks"));
        dataverse.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
        dataverse.setModificationTime(Timestamp.valueOf(LocalDateTime.now()));

        return dataverse;
    }
}
