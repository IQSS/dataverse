package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@RunWith(Arquillian.class)
public class DataverseLinkingServiceIT extends WebappArquillianDeployment {

    @Inject
    private DataverseLinkingService dataverseLinkingService;
    @Inject
    private DataverseRepository dataverseRepository;
    @Inject
    private UserServiceBean userService;
    @Inject
    private DataverseSession dataverseSession;

    // -------------------- TESTS --------------------

    @Test
    public void saveLinkedDataverse() {
        //given
        loginSessionWithSuperUser();
        Dataverse ownerDataverse = dataverseRepository.getById(19L);
        Dataverse dataverseToBeLinked = dataverseRepository.getById(51L);

        //when
        dataverseLinkingService.saveLinkedDataverse(dataverseToBeLinked, ownerDataverse);
        Dataverse linkedDataverse = dataverseRepository.getById(dataverseToBeLinked.getId());

        //then
        Assert.assertTrue(retrieveLinkedDataverseOwners(linkedDataverse).contains(ownerDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveLinkedDataverse_WithIllegalCommandEx() {
        //given
        loginSessionWithSuperUser();
        Dataverse ownerDataverse = dataverseRepository.getById(19L);

        //when & then
        Assertions.assertThrows(IllegalCommandException.class, () -> dataverseLinkingService.saveLinkedDataverse(ownerDataverse, ownerDataverse));
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void saveLinkedDataverse_WithPermissionException() {
        //given
        Dataverse ownerDataverse = dataverseRepository.getById(19L);
        Dataverse dataverseToBeLinked = dataverseRepository.getById(51L);

        //when & then
        Assertions.assertThrows(PermissionException.class, () -> dataverseLinkingService.saveLinkedDataverse(dataverseToBeLinked, ownerDataverse));
    }

    // -------------------- PRIVATE --------------------

    private long loginSessionWithSuperUser() {
        AuthenticatedUser user = userService.find(2L);
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
}
