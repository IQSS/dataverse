package edu.harvard.iq.dataverse.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import io.vavr.control.Either;
import org.primefaces.model.DualListModel;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DataverseService {

    private static final Logger logger = Logger.getLogger(DataverseService.class.getCanonicalName());

    @Inject
    private DataverseSession session;

    @Inject
    private DataverseRequestServiceBean dvRequestService;

    @Inject
    private EjbDataverseEngine commandEngine;

    @Inject
    private UserNotificationService userNotificationService;

    // -------------------- LOGIC --------------------

    public Either<DataverseError, Dataverse> saveNewDataverse(Collection<DataverseFieldTypeInputLevel> dftilToBeSaved,
                                                              Dataverse dataverse,
                                                              DualListModel<DatasetFieldType> facets) {

        if (!dataverse.isFacetRoot()) {
            facets.getTarget().clear();
        }

        if (session.getUser().isAuthenticated()) {
            Command<Dataverse> cmd = new CreateDataverseCommand(dataverse,
                                                                dvRequestService.getDataverseRequest(),
                                                                facets.getTarget(),
                                                                Lists.newArrayList(dftilToBeSaved));

            try {
                dataverse = commandEngine.submit(cmd);
            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
                return Either.left(new DataverseError(ex, BundleUtil.getStringFromBundle("dataverse.create.failure")));
            }

            userNotificationService.sendNotificationWithEmail((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(),
                                                              NotificationType.CREATEDV,
                                                              dataverse.getId(), NotificationObjectType.DATAVERSE);
        } else {
            return Either.left(new DataverseError(BundleUtil.getStringFromBundle("dataverse.create.authenticatedUsersOnly")));
        }

        return Either.right(dataverse);
    }

    public Either<DataverseError, Dataverse> saveEditedDataverse(Collection<DataverseFieldTypeInputLevel> dftilToBeSaved,
                                                                 Dataverse dataverse,
                                                                 DualListModel<DatasetFieldType> facets) {

        if (!dataverse.isFacetRoot()) {
            facets.getTarget().clear();
        }

        UpdateDataverseCommand cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), null,
                                                                dvRequestService.getDataverseRequest(), Lists.newArrayList(dftilToBeSaved));

        try {
            dataverse = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
            return Either.left(new DataverseError(ex, BundleUtil.getStringFromBundle("dataverse.create.failure")));
        }

        return Either.right(dataverse);
    }

    public Dataverse saveFeaturedDataverse(Dataverse dataverse, List<Dataverse> featuredDataverses) {

        return commandEngine.submit(new UpdateDataverseCommand(dataverse, null,
                                                               featuredDataverses,
                                                               dvRequestService.getDataverseRequest(),
                                                               null));
    }

    /**
     * Operation to link one dataverse to the other.
     */
    public DataverseLinkingDataverse saveLinkedDataverse(Dataverse dataverseToBeLinked, Dataverse dataverse) {

        return commandEngine.submit(new LinkDataverseCommand(dvRequestService.getDataverseRequest(), dataverseToBeLinked, dataverse));
    }

    /**
     * Operation to publish dataverse also known as release dataverse.
     */
    public Dataverse publishDataverse(Dataverse dataverseToBePublished) {

        return commandEngine.submit(new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataverseToBePublished));
    }

    public void deleteDataverse(Dataverse dataverseToDelete){
        commandEngine.submit(new DeleteDataverseCommand(dvRequestService.getDataverseRequest(), dataverseToDelete));
    }

}
