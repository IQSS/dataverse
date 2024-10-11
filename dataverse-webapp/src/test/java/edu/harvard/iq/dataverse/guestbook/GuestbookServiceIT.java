package edu.harvard.iq.dataverse.guestbook;


import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.stream.Collectors;

@Transactional(TransactionMode.ROLLBACK)
public class GuestbookServiceIT extends WebappArquillianDeployment {

    @EJB
    private GuestbookService guestbookService;

    @Inject
    private DataverseDao dataverseDao;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @BeforeEach
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldSaveGuestbook() {
        // given
        Dataverse dataverse = dataverseDao.findByAlias("ownmetadatablocks");
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setName("newGuestbook");
        newGuestbook.setDataverse(dataverse);

        // when
        guestbookService.saveGuestbook(newGuestbook);

        // then
        Dataverse dbDataverse = dataverseDao.find(dataverse.getId());
        Assertions.assertEquals(2, dataverse.getGuestbooks().size());
        Assertions.assertTrue(dbDataverse.getGuestbooks()
                .stream()
                .map(Guestbook::getName).collect(Collectors.toList())
                .contains("newGuestbook"));
    }

    @Test
    public void shouldEditGuestbook() {
        // given
        Dataverse dataverse = dataverseDao.findByAlias("ownmetadatablocks");
        Guestbook guestbook = dataverse.getGuestbooks().get(0);
        guestbook.setName("editedGuestbookName");

        // when
        guestbookService.editGuestbook(guestbook);

        // then
        Dataverse dbDataverse = dataverseDao.find(dataverse.getId());
        Assertions.assertEquals(1, dbDataverse.getGuestbooks().size());
        Assertions.assertEquals(guestbook.getName(), dbDataverse.getGuestbooks().get(0).getName());
    }
}
