package edu.harvard.iq.dataverse.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import io.vavr.control.Either;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.primefaces.model.DualListModel;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static edu.harvard.iq.dataverse.search.DvObjectsSolrAssert.assertDataversePermSolrDocument;
import static edu.harvard.iq.dataverse.search.DvObjectsSolrAssert.assertDataverseSolrDocument;
import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
public class DataverseServiceIT extends WebappArquillianDeployment {

    @Inject
    private DataverseService dataverseService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseDao dataverseDao;

    @Inject
    private UserServiceBean userServiceBean;

    @Inject
    private SolrClient solrClient;

    @Before
    public void init() throws SolrServerException, IOException, SQLException {
        FacesContextMocker.mockServletRequest();
        solrClient.deleteByQuery("*:*");
        solrClient.commit();
    }

    // -------------------- TESTS --------------------

    @Test
    public void saveNewDataverse_ShouldSuccessfullySave() throws SolrServerException, IOException {
        //given
        long userId = loginSessionWithSuperUser();
        Dataverse dataverse = prepareDataverse();

        //when
        Either<DataverseError, Dataverse> savedDataverse = dataverseService.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());


        //then
        Assert.assertTrue(savedDataverse.isRight());

        Dataverse dbDataverse = dataverseDao.find(savedDataverse.get().getId());
        Assert.assertEquals("NICE DATAVERSE", dbDataverse.getName());

        await()
                .atMost(Duration.ofSeconds(5L))
                .until(() -> smtpServer.mailBox().stream()
                        .anyMatch(emailModel -> emailModel.getSubject().contains("Your dataverse has been created")));


        SolrDocument dataverseSolrDoc = solrClient.getById("dataverse_" + savedDataverse.get().getId());
        assertDataverseSolrDocument(dataverseSolrDoc, savedDataverse.get().getId(), "FIRSTDATAVERSE", "NICE DATAVERSE");

        SolrDocument dataversePermSolrDoc = solrClient.getById("dataverse_" + savedDataverse.get().getId() + "_permission");
        assertDataversePermSolrDocument(dataversePermSolrDoc, savedDataverse.get().getId(), Lists.newArrayList(userId));

    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveNewDataverse_WithWrongUser() {
        //given
        Dataverse dataverse = prepareDataverse();
        dataverseSession.setUser(GuestUser.get());

        //when
        Either<DataverseError, Dataverse> savedDataverse = dataverseService.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(savedDataverse.isLeft());
        Assert.assertEquals(3, dataverseDao.findAll().size());

    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveEditedDataverse() {
        //given
        loginSessionWithSuperUser();
        Dataverse dataverse = dataverseDao.findRootDataverse();
        String oldDataverseName = dataverse.getName();
        dataverse.setName("UPDATED DATAVERSE");

        //when
        Either<DataverseError, Dataverse> updatedDataverse = dataverseService.saveEditedDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertNotEquals(oldDataverseName, updatedDataverse.get().getName());

    }

    @Test(expected = EJBTransactionRolledbackException.class)
    @Transactional(TransactionMode.ROLLBACK)
    public void saveEditedDataverse_WithNonExistingDataverse() {
        //given
        Dataverse dataverse = new Dataverse();

        //when
        Either<DataverseError, Dataverse> updatedDataverse = dataverseService.saveEditedDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(updatedDataverse.isLeft());
        Assert.assertEquals(1, dataverseDao.findAll().size());

    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveLinkedDataverse() {
        //given
        loginSessionWithSuperUser();
        Dataverse ownerDataverse = dataverseDao.find(19L);
        Dataverse dataverseToBeLinked = dataverseDao.find(51L);

        //when
        dataverseService.saveLinkedDataverse(dataverseToBeLinked, ownerDataverse);
        Dataverse linkedDataverse = dataverseDao.find(dataverseToBeLinked.getId());

        //then
        Assert.assertTrue(retrieveLinkedDataverseOwners(linkedDataverse).contains(ownerDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveLinkedDataverse_WithIllegalCommandEx() {
        //given
        loginSessionWithSuperUser();
        Dataverse ownerDataverse = dataverseDao.find(19L);

        //when & then
        Assertions.assertThrows(IllegalCommandException.class, () -> dataverseService.saveLinkedDataverse(ownerDataverse, ownerDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveLinkedDataverse_WithPermissionException() {
        //given
        Dataverse ownerDataverse = dataverseDao.find(19L);
        Dataverse dataverseToBeLinked = dataverseDao.find(51L);

        //when & then
        Assertions.assertThrows(PermissionException.class, () -> dataverseService.saveLinkedDataverse(dataverseToBeLinked, ownerDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void publishDataverse() {
        //given
        loginSessionWithSuperUser();
        Dataverse unpublishedDataverse = dataverseDao.findRootDataverse();

        //when
        dataverseService.publishDataverse(unpublishedDataverse);
        Dataverse publishedDataverse = dataverseDao.find(unpublishedDataverse.getId());

        //then
        Assert.assertTrue(publishedDataverse.isReleased());
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void publishDataverse_WithIllegalCommandException() {
        //given
        loginSessionWithSuperUser();
        Dataverse unpublishedDataverse = dataverseDao.findRootDataverse();
        unpublishedDataverse.setPublicationDate(Timestamp.from(Instant.ofEpochMilli(1573738827897L)));

        //when & then
        Assertions.assertThrows(IllegalCommandException.class, () -> dataverseService.publishDataverse(unpublishedDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void deleteDataverse() {
        //given
        loginSessionWithSuperUser();
        Dataverse unpublishedDataverse = dataverseDao.find(19L);

        //when
        dataverseService.deleteDataverse(unpublishedDataverse);

        //then
        Assert.assertNull(dataverseDao.find(19L));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void deleteDataverse_WithIllegalCommandException() {
        //given
        loginSessionWithSuperUser();
        Dataverse unpublishedDataverse = dataverseDao.find(19L);
        unpublishedDataverse.setOwner(null);

        //when
        Assertions.assertThrows(IllegalCommandException.class, () -> dataverseService.deleteDataverse(unpublishedDataverse));
    }

    // -------------------- PRIVATE --------------------

    private long loginSessionWithSuperUser() {
        AuthenticatedUser user = userServiceBean.find(2L);
        dataverseSession.setUser(user);
        return user.getId();
    }

    /**
     * Helper filtering method, because for some reason streams won't work in this case.
     */
    private List<Dataverse> retrieveLinkedDataverseOwners(Dataverse linkedDataverse){
        List<DataverseLinkingDataverse> dataverseLinkedDataverses = linkedDataverse.getDataverseLinkedDataverses();

        List<Dataverse> linkedOwners = new ArrayList<>();
        for (DataverseLinkingDataverse dataverseLinkedDatavers : dataverseLinkedDataverses) {
            linkedOwners.add(dataverseLinkedDatavers.getDataverse());
        }

        return linkedOwners;
    }

    private Dataverse prepareDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setMetadataBlockRoot(true);
        dataverse.setOwner(dataverseDao.findRootDataverse());
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
}