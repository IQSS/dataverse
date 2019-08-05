package edu.harvard.iq.dataverse.dataverse;

import com.google.api.client.util.Lists;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.DataverseArquillian;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.ArquillianDeployment;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import io.vavr.control.Either;
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

import javax.ejb.AsyncResult;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(DataverseArquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DataverseSaverIT extends ArquillianDeployment {

    @Inject
    private DataverseSaver dataverseSaver;

    @Mock
    private IndexServiceBean indexServiceBean;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseServiceBean dataverseServiceBean;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        FacesContextMocker.mockServletRequest();

        when(indexServiceBean.indexDataverse(Mockito.any(Dataverse.class)))
                .thenReturn(new AsyncResult<>("NICE"));

        dataverseSession.setUser(createUser());
    }

    @Test
    public void saveNewDataverse_ShouldSuccessfullySave() {
        //given
        Dataverse dataverse = prepareDataverse();

        //when
        Either<DataverseError, Dataverse> savedDataverse = dataverseSaver.saveNewDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertTrue(savedDataverse.isRight());
        Assert.assertEquals(2, dataverseServiceBean.findAll().size());
    }

    @Test
    public void saveEditedDataverse() {
        //given
        Dataverse dataverse = dataverseServiceBean.findRootDataverse();
        String oldDataverseName = dataverse.getName();
        dataverse.setName("UPDATED DATAVERSE");

        //when
        Either<DataverseError, Dataverse> updatedDataverse = dataverseSaver.saveEditedDataverse(Lists.newArrayList(), dataverse, new DualListModel<>());

        //then
        Assert.assertNotEquals(oldDataverseName, updatedDataverse.get().getName());

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