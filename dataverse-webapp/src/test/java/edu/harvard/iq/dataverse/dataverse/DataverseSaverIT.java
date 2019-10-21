package edu.harvard.iq.dataverse.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
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
import org.junit.runner.RunWith;
import org.primefaces.model.DualListModel;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static edu.harvard.iq.dataverse.search.DvObjectsSolrAssert.assertDataversePermSolrDocument;
import static edu.harvard.iq.dataverse.search.DvObjectsSolrAssert.assertDataverseSolrDocument;
import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
public class DataverseSaverIT extends WebappArquillianDeployment {

    @Inject
    private DataverseSaver dataverseSaver;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseServiceBean dataverseServiceBean;
    
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
        Either<DataverseError, Dataverse> savedDataverse = dataverseSaver.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());
        

        //then
        Assert.assertTrue(savedDataverse.isRight());
        
        Dataverse dbDataverse = dataverseServiceBean.find(savedDataverse.get().getId());
        Assert.assertEquals("NICE DATAVERSE", dbDataverse.getName());

        await()
                .atMost(Duration.ofSeconds(3L))
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
        Either<DataverseError, Dataverse> savedDataverse = dataverseSaver.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(savedDataverse.isLeft());
        Assert.assertEquals(3, dataverseServiceBean.findAll().size());

    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveEditedDataverse() {
        //given
        loginSessionWithSuperUser();
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

    // -------------------- PRIVATE --------------------
    
    private long loginSessionWithSuperUser() {
        AuthenticatedUser user = userServiceBean.find(2L);
        dataverseSession.setUser(user);
        return user.getId();
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
}