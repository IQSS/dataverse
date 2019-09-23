package edu.harvard.iq.dataverse.dataverse;

import com.google.api.client.util.Lists;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.config.StartupFlywayMigrator;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import io.vavr.control.Either;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.primefaces.model.DualListModel;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@RunWith(Arquillian.class)
public class DataverseSaverIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Resource
    private UserTransaction transaction;

    @Inject
    private DataverseSaver dataverseSaver;

    @Mock
    private IndexServiceBean indexServiceBean;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseServiceBean dataverseServiceBean;

    @Inject
    private StartupFlywayMigrator startupFlywayMigrator;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        FacesContextMocker.mockServletRequest();

        when(indexServiceBean.indexDataverse(Mockito.any(Dataverse.class)))
                .thenReturn(new AsyncResult<>("NICE"));
    }

    @Test
    public void saveNewDataverse_ShouldSuccessfullySave() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        //given
        transaction.begin();
        createSessionUser();

        Dataverse dataverse = prepareDataverse();

        //when
        Either<DataverseError, Dataverse> savedDataverse = dataverseSaver.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());
        transaction.commit();

        //then
        Assert.assertTrue(savedDataverse.isRight());
        Assert.assertEquals(2, dataverseServiceBean.findAll().size());

        await()
                .atMost(Duration.ofSeconds(3L))
                .until(() -> smtpServer.mailBox().stream()
                        .anyMatch(emailModel -> emailModel.getSubject().contains("Your dataverse has been created")));

        cleanupDatabase();
    }

    private void createSessionUser() {
        AuthenticatedUser user = createUser();
        em.persist(user);
        dataverseSession.setUser(user);
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveNewDataverse_WithWrongUser() {
        //given
        Dataverse dataverse = prepareDataverse();
        dataverseSession.setUser(GuestUser.get());

        //when
        Either<DataverseError, Dataverse> savedDataverse = dataverseSaver.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(savedDataverse.isLeft());
        Assert.assertEquals(1, dataverseServiceBean.findAll().size());

    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveEditedDataverse() {
        //given
        createSessionUser();
        Dataverse dataverse = dataverseServiceBean.findRootDataverse();
        String oldDataverseName = dataverse.getName();
        dataverse.setName("UPDATED DATAVERSE");

        //when
        Either<DataverseError, Dataverse> updatedDataverse = dataverseSaver.saveEditedDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertNotEquals(oldDataverseName, updatedDataverse.get().getName());

    }

    @Test(expected = EJBTransactionRolledbackException.class)
    @Transactional(TransactionMode.ROLLBACK)
    public void saveEditedDataverse_WithNonExistingDataverse() {
        //given
        Dataverse dataverse = new Dataverse();

        //when
        Either<DataverseError, Dataverse> updatedDataverse = dataverseSaver.saveEditedDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(updatedDataverse.isLeft());
        Assert.assertEquals(1, dataverseServiceBean.findAll().size());

    }

    private void cleanupDatabase() throws NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        transaction.begin();

        dropAllTables();
        startupFlywayMigrator.migrateDatabase();

        transaction.commit();
    }

    private void dropAllTables() {
        em.createNativeQuery("DO $$ DECLARE\n" +
                                     "    r RECORD;\n" +
                                     "BEGIN\n" +
                                     "    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP\n" +
                                     "        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n" +
                                     "    END LOOP;\n" +
                                     "END $$;").executeUpdate();
    }

    private Dataverse prepareDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setMetadataBlockRoot(true);
        dataverse.setOwner(dataverseServiceBean.findRootDataverse());
        dataverse.setName("NICE DATAVERSE");
        dataverse.setAlias("FIRSTDATAVERSE");
        dataverse.setFacetRoot(true);
        dataverse.setDataverseType(Dataverse.DataverseType.JOURNALS);
        dataverse.setDataverseContacts(prepareDataverseContact());
        dataverse.setAllowMessagesBanners(false);

        return dataverse;
    }

    private List<DataverseContact> prepareDataverseContact() {
        DataverseContact dataverseContact = new DataverseContact();
        dataverseContact.setContactEmail("test@gmail.com");

        ArrayList<DataverseContact> contacts = new ArrayList<>();
        contacts.add(dataverseContact);
        return contacts;
    }

    private AuthenticatedUser createUser() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setSuperuser(true);
        user.setLastName("Banan");
        user.setEmail("test@gmail.com");
        user.setUserIdentifier("TERMINATOR");
        user.setFirstName("Anakin");
        user.setCreatedTime(Timestamp.valueOf(LocalDateTime.of(2019, 1, 1, 1, 1)));
        return user;
    }
}