package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookRootCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ManageGuestbooksService {
    private EjbDataverseEngine engineService;
    private DataverseRequestServiceBean dvRequestService;
    private GuestbookServiceBean guestbookService;
    private DataverseServiceBean dataverseService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageGuestbooksService() {
    }

    @Inject
    public ManageGuestbooksService(EjbDataverseEngine engineService, DataverseRequestServiceBean dvRequestService,
                                   GuestbookServiceBean guestbookService, DataverseServiceBean dataverseService) {
        this.engineService = engineService;
        this.dvRequestService = dvRequestService;
        this.guestbookService = guestbookService;
        this.dataverseService = dataverseService;
    }

    // -------------------- LOGIC --------------------
    public Dataverse deleteGuestbook(long guestbookId) {
        Guestbook guestbook = guestbookService.find(guestbookId);
        Dataverse dataverse = guestbook.getDataverse();
        dataverse.getGuestbooks().remove(guestbook);

        return engineService.submit(new DeleteGuestbookCommand(dvRequestService.getDataverseRequest(), dataverse, guestbook));
    }

    public Dataverse updateAllowGuestbooksFromRootStatus(long dataverseId, boolean allowGuestbooksFromRoot) {
        return engineService.submit(new UpdateDataverseGuestbookRootCommand(allowGuestbooksFromRoot, dvRequestService.getDataverseRequest(), dataverseService.find(dataverseId)));
    }

    public Guestbook enableGuestbook(long guestbookId) {
        Guestbook guestbook = guestbookService.find(guestbookId);
        guestbook.setEnabled(true);
        updateDataverse(guestbook.getDataverse());

        return guestbook;
    }

    public Guestbook disableGuestbook(long guestbookId) {
        Guestbook guestbook = guestbookService.find(guestbookId);
        guestbook.setEnabled(false);
        updateDataverse(guestbook.getDataverse());

        return guestbook;
    }

    // -------------------- PRIVATE ---------------------
    private Dataverse updateDataverse(Dataverse dataverse) {
        return engineService.submit(new UpdateDataverseCommand(dataverse, null, null, dvRequestService.getDataverseRequest(), null));
    }
}
