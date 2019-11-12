package edu.harvard.iq.dataverse.guestbook;


import edu.harvard.iq.dataverse.DataverseServiceBean;
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
import java.util.stream.Collectors;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class GuestbookServiceIT extends WebappArquillianDeployment {

    @EJB
    private GuestbookService guestbookService;

    @Inject
    private DataverseServiceBean dataverseService;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldSaveGuestbook() {
        // given
        Dataverse dataverse = dataverseService.findByAlias("ownmetadatablocks");
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setName("newGuestbook");
        newGuestbook.setDataverse(dataverse);

        // when
        guestbookService.saveGuestbook(newGuestbook);

        // then
        Dataverse dbDataverse = dataverseService.find(dataverse.getId());
        Assert.assertEquals(2, dataverse.getGuestbooks().size());
        Assert.assertTrue(dbDataverse.getGuestbooks()
                .stream()
                .map(Guestbook::getName).collect(Collectors.toList())
                .contains("newGuestbook"));
    }

    @Test
    public void shouldEditGuestbook() {
        // given
        Dataverse dataverse = dataverseService.findByAlias("ownmetadatablocks");
        Guestbook guestbook = dataverse.getGuestbooks().get(0);
        guestbook.setName("editedGuestbookName");

        // when
        guestbookService.editGuestbook(guestbook);

        // then
        Dataverse dbDataverse = dataverseService.find(dataverse.getId());
        Assert.assertEquals(1, dbDataverse.getGuestbooks().size());
        Assert.assertEquals(guestbook.getName(), dbDataverse.getGuestbooks().get(0).getName());
    }
}
