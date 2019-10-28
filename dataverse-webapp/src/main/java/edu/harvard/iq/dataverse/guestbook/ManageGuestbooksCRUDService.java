package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookRootCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Logger;

@Stateless
public class ManageGuestbooksCRUDService {
    private static final Logger logger = Logger.getLogger(ManageGuestbooksCRUDService.class.getCanonicalName());

    private EjbDataverseEngine engineService;
    private DataverseRequestServiceBean dvRequestService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageGuestbooksCRUDService() {
    }

    @Inject
    public ManageGuestbooksCRUDService(EjbDataverseEngine engineService, DataverseRequestServiceBean dvRequestService) {
        this.engineService = engineService;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------
    public Try<Dataverse> createOrUpdate(Dataverse dataverse) {
        return Try.of(() -> engineService.submit(new UpdateDataverseCommand(dataverse, null, null, dvRequestService.getDataverseRequest(), null)))
                .onFailure(throwable -> logger.warning("Failed to create or update guestbooks for dataverse: " + dataverse.getName()));
    }

    public Try<Dataverse> delete(Dataverse dataverse, Guestbook guestbook) {
        return Try.of(() -> engineService.submit(new DeleteGuestbookCommand(dvRequestService.getDataverseRequest(), dataverse, guestbook)))
                .onFailure(throwable -> logger.warning("Failed to delete guestbook: " + guestbook.getName() + " from dataverse: " + dataverse.getName()));
    }

    public Try<Dataverse> updateRoot(Dataverse dataverse) {
        return Try.of(() -> engineService.submit(new UpdateDataverseGuestbookRootCommand(dataverse.isGuestbookRoot(), dvRequestService.getDataverseRequest(), dataverse)))
                .onFailure(throwable -> logger.warning("Failed to update root guestbook for dataverse: " + dataverse.getName()));
    }
}
