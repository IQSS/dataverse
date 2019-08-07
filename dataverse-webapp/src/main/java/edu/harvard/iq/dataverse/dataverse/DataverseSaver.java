package edu.harvard.iq.dataverse.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import io.vavr.control.Either;
import org.primefaces.model.DualListModel;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DataverseSaver {

    private static final Logger logger = Logger.getLogger(DataverseSaver.class.getCanonicalName());

    @Inject
    private DataverseSession session;

    @Inject
    private DataverseRequestServiceBean dvRequestService;

    @Inject
    private DataverseServiceBean dataverseService;

    @Inject
    private EjbDataverseEngine commandEngine;

    @Inject
    private UserNotificationServiceBean userNotificationService;

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

            sendSuccessNotificationAsync(dataverse, session.getUser());
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

    // -------------------- PRIVATE --------------------

    private void sendSuccessNotificationAsync(Dataverse dataverse, User user) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() ->
                                        userNotificationService.sendNotification((AuthenticatedUser) user, dataverse.getCreateDate(),
                                                                                 UserNotification.Type.CREATEDV, dataverse.getId()));

        executorService.shutdown();
    }
}
